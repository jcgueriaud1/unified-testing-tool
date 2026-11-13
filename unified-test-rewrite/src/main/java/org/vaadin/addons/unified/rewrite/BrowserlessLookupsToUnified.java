package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.List;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * Turns Browserless lookups into Unified Page lookups, in methods that have the {@code UnifiedPage page}
 * parameter: {@code $(TextField.class).withCaption("Name").single()} becomes
 * {@code page.find(UnifiedTextField.class).withLabel("Name").single()}, {@code test(component).setValue(..)} becomes
 * {@code component.setValue(..)}, component getters take their Wrapper names, and locals the Wrapper's type. A
 * lookup converts only when the Wrapper has every call made on what it finds; {@code first()}, {@code last()} and
 * {@code atIndex(..)} become {@code single()} with a Migration Marker.
 */
public class BrowserlessLookupsToUnified extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate Browserless lookups and tester calls to Unified Page lookups";
    }

    @Override
    public String getDescription() {
        return "Turns `$(X.class)` queries into `page.find(UnifiedX.class)` and `test(component).call(..)` into "
                + "Wrapper calls, for components that have a Wrapper with every call made on them.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (!Pages.hasPageParameter(method)) {
                    return method;
                }
                var markers = MigrationMarkers.collect(getCursor());
                return MigrationMarkers.write(super.visitMethodDeclaration(method, ctx), markers);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                Cursor method = getCursor().dropParentUntil(t -> t instanceof J.MethodDeclaration || t == Cursor.ROOT_VALUE);
                Browserless.Wrapper wrapper = !(method.getValue() instanceof J.MethodDeclaration m) || !Pages.hasPageParameter(m)
                        || multiVariable.getVariables().size() != 1 || multiVariable.getTypeExpression() == null
                        ? null : Browserless.convertibleLocal(method, multiVariable.getVariables().get(0));
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                if (wrapper == null) {
                    return v;
                }
                maybeAddImport(wrapper.fqn());
                maybeRemoveImport(fqn(multiVariable.getTypeExpression().getType()));
                JavaType.ShallowClass type = JavaType.ShallowClass.build(wrapper.fqn());
                return v.withTypeExpression(new J.Identifier(Tree.randomId(), v.getTypeExpression().getPrefix(),
                                Markers.EMPTY, emptyList(), wrapper.simpleName(), type, null))
                        .withVariables(List.of(v.getVariables().get(0).withType(type)
                                .withName(v.getVariables().get(0).getName().withType(type).withFieldType(null))));
            }

            /** A use of a local that takes the Wrapper's type: it takes that type too. */
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier id = super.visitIdentifier(identifier, ctx);
                if (id.getFieldType() == null || id.getFieldType().getOwner() instanceof JavaType.FullyQualified
                        || !inPageMethod()
                        || getCursor().getParentTreeCursor().getValue() instanceof J.VariableDeclarations.NamedVariable) {
                    return id;
                }
                Browserless.Wrapper wrapper = Browserless.convertibleLocal(getCursor(), identifier);
                return wrapper == null ? id
                        : id.withType(JavaType.ShallowClass.build(wrapper.fqn())).withFieldType(null);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!inPageMethod()) {
                    return m;
                }
                // test(component).setValue(..) → component.setValue(..)
                if (method.getSelect() instanceof J.MethodInvocation test && Browserless.isTest(test)
                        && m.getSelect() instanceof J.MethodInvocation convertedTest) {
                    Browserless.Wrapper wrapper = Browserless.convertibleElement(
                            new Cursor(getCursor(), test), test.getArguments().get(0));
                    String unified = wrapper == null ? null : wrapper.testerCalls().get(method.getSimpleName());
                    if (unified != null) {
                        Expression component = convertedTest.getArguments().get(0).withPrefix(convertedTest.getPrefix());
                        // no Browserless types left behind, so their imports go
                        return unifiedCall(m.getPadding().withSelect(JRightPadded.build(component)), wrapper.fqn(),
                                unified, m.getMethodType() == null ? null : m.getMethodType().getReturnType());
                    }
                    return m;
                }
                // component.getValue() → component.getValue(), checkbox.getValue() → checkbox.isChecked()
                if (method.getSelect() != null && Browserless.noArguments(method)) {
                    Browserless.Wrapper wrapper = Browserless.convertibleElement(getCursor(), method.getSelect());
                    String unified = wrapper == null ? null : wrapper.getters().get(method.getSimpleName());
                    if (unified != null) {
                        return unifiedCall(m, wrapper.fqn(), unified,
                                m.getMethodType() == null ? null : m.getMethodType().getReturnType());
                    }
                }
                Cursor terminal = Browserless.terminalOf(getCursor());
                Browserless.Wrapper wrapper = terminal == null ? null : Browserless.convertibleLookup(terminal);
                if (wrapper == null) {
                    return m;
                }
                if (Browserless.ofLookupStart(method) != null) {
                    String component = ((J.FieldAccess) method.getArguments().get(0)).getTarget().getType()
                            instanceof JavaType.FullyQualified t ? t.getFullyQualifiedName() : null;
                    maybeAddImport(wrapper.fqn());
                    maybeRemoveImport(component);
                    return JavaTemplate.builder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}.find("
                                    + wrapper.simpleName() + ".class)")
                            .imports(wrapper.fqn())
                            .javaParser(Stubs.parser(Stubs.interfaceStub(wrapper.fqn())))
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace(), Stubs.page());
                }
                // the query's calls keep no Browserless types, so their imports go
                boolean isTerminal = terminal.getValue() == method;
                m = unifiedCall(m, QUERY, m.getSimpleName(),
                        JavaType.ShallowClass.build(isTerminal ? wrapper.fqn() : QUERY));
                if (isTerminal) {
                    if (method.getSimpleName().equals("single")
                            || !MigrationMarkers.add(getCursor(), "was " + printCall(method)
                                    + "; single() fails when several components match")) {
                        return m;
                    }
                    J.MethodInvocation single = unifiedCall(m, QUERY, "single", JavaType.ShallowClass.build(wrapper.fqn()));
                    if (single.getMethodType() != null) {
                        single = single.withMethodType(single.getMethodType().withParameterTypes(List.of())
                                .withParameterNames(List.of()));
                        single = single.withName(single.getName().withType(single.getMethodType()));
                    }
                    return single.withArguments(List.of(new J.Empty(
                            Tree.randomId(), Space.EMPTY, Markers.EMPTY)));
                }
                String filter = wrapper.filters().get(method.getSimpleName());
                if (wrapper.isButton() && method.getSimpleName().equals("withCaption")
                        && !MigrationMarkers.add(getCursor(), "was " + printCall(method)
                                + "; use withAccessibleName(..) for a button without text")) {
                    return m;
                }
                return filter == null ? m : unifiedCall(m, QUERY, filter, JavaType.ShallowClass.build(QUERY));
            }

            /** The call alone, without what it is called on: {@code first()}, {@code withCaption("Save")}. */
            private String printCall(J.MethodInvocation call) {
                return call.printTrimmed(getCursor()).replaceFirst("^[\\s\\S]*\\.(?=" + call.getSimpleName() + "\\()", "");
            }

            private boolean inPageMethod() {
                J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return enclosing != null && Pages.hasPageParameter(enclosing);
            }
        };
    }

    private static final String QUERY = "com.vaadin.testing.unified.query.Query";

    /**
     * The call renamed, with a method type on the Unified type: no Browserless type is left in it (so its import
     * goes), and the tree keeps the type information later recipes rely on.
     */
    private static J.MethodInvocation unifiedCall(J.MethodInvocation m, String declaringType, String name,
            JavaType returnType) {
        JavaType.Method type = m.getMethodType();
        if (type == null) {
            return m.withName(m.getName().withSimpleName(name));
        }
        List<JavaType> parameters = type.getParameterTypes().stream()
                .map(p -> p instanceof JavaType.Primitive || TypeUtils.isString(p) ? p
                        : (JavaType) JavaType.ShallowClass.build("java.lang.Object"))
                .toList();
        JavaType returned = returnType instanceof JavaType.Primitive || TypeUtils.isString(returnType)
                || returnType instanceof JavaType.ShallowClass ? returnType : JavaType.Primitive.Void;
        JavaType.Method unified = type.withDeclaringType(JavaType.ShallowClass.build(declaringType)).withName(name)
                .withReturnType(returned).withParameterTypes(parameters);
        return m.withMethodType(unified).withName(m.getName().withSimpleName(name).withType(unified));
    }

    private static String fqn(JavaType type) {
        return type instanceof JavaType.FullyQualified f ? f.getFullyQualifiedName() : null;
    }
}
