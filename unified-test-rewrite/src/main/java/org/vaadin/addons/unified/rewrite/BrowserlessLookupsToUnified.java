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
                boolean candidate = method.getValue() instanceof J.MethodDeclaration m && Pages.hasPageParameter(m)
                        && multiVariable.getVariables().size() == 1 && multiVariable.getTypeExpression() != null;
                Browserless.Wrapper wrapper = candidate ? Browserless.convertibleLocal(method, multiVariable.getVariables().get(0)) : null;
                Browserless.Wrapper tester = candidate && wrapper == null
                        ? Browserless.testerLocal(method, multiVariable.getVariables().get(0)) : null;
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                if (tester != null) {
                    // GridTester t = test(grid) → UnifiedGrid t = grid; a locator local is already a lookup
                    wrapper = tester;
                    if (v.getVariables().get(0).getInitializer() instanceof J.MethodInvocation test
                            && multiVariable.getVariables().get(0).getInitializer() instanceof J.MethodInvocation original
                            && Browserless.isTest(original)) {
                        Expression component = test.getArguments().get(0).withPrefix(test.getPrefix());
                        v = v.withVariables(List.of(v.getVariables().get(0).withInitializer(component)));
                    }
                }
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
                if (wrapper == null) {
                    wrapper = Browserless.testerLocal(getCursor(), identifier);
                }
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
                    String unified = wrapper == null ? null : Browserless.operation(wrapper, method);
                    if (unified != null) {
                        Expression component = convertedTest.getArguments().get(0).withPrefix(convertedTest.getPrefix());
                        // no Browserless types left behind, so their imports go
                        return operation(m, component, wrapper, method, unified);
                    }
                    return m;
                }
                // a tester or locator local: t.size() → t.getRowCount(), t.clickRow(i) → t.row(i).click()
                if (method.getSelect() instanceof J.Identifier local && !(m.getSelect() instanceof J.MethodInvocation)) {
                    Browserless.Wrapper wrapper = Browserless.testerLocal(getCursor(), local);
                    String unified = wrapper == null ? null : Browserless.operation(wrapper, method);
                    if (unified != null) {
                        return operation(m, m.getSelect(), wrapper, method, unified);
                    }
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
                // a locator of the context API: window.findTextField().withLabel("Name") → page.find(..).withLabel("Name")
                if (Browserless.ofLocator(method) != null) {
                    Cursor locator = Browserless.locatorOf(getCursor());
                    Browserless.Wrapper w = locator == null ? null : Browserless.convertibleLocator(locator);
                    if (w == null) {
                        return m;
                    }
                    J.MethodInvocation converted;
                    if (Browserless.ofLocatorStart(method) != null) {
                        maybeAddImport(w.fqn());
                        converted = JavaTemplate.builder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}.find("
                                        + w.simpleName() + ".class)")
                                .imports(w.fqn())
                                .javaParser(Stubs.parser(Stubs.interfaceStub(w.fqn())))
                                .build()
                                .apply(getCursor(), m.getCoordinates().replace(), Stubs.page());
                    } else {
                        converted = unifiedCall(m, QUERY, w.filters().get(method.getSimpleName()),
                                JavaType.ShallowClass.build(QUERY));
                    }
                    // a locator is lazy and unique; a Unified lookup ends in single()
                    Object locatorParent = locator.getParentTreeCursor().getValue();
                    boolean asserted = locator.getValue() == method
                            && (TestBenchAssertionsToUnified.isUnifiedAssertion(locatorParent)
                                    || locatorParent instanceof J.VariableDeclarations.NamedVariable);
                    return asserted ? single(converted, w) : converted;
                }
                // ..locator.setValue(..) → ..single().setValue(..)
                if (method.getSelect() != null && Browserless.ofLocator(method.getSelect()) != null) {
                    Browserless.Wrapper w = Browserless.convertibleLocator(new Cursor(getCursor(), method.getSelect()));
                    String unified = w == null ? null : Browserless.operation(w, method);
                    if (unified == null) {
                        return m;
                    }
                    return operation(m, single(m.getSelect(), w), w, method, unified);
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

            /**
             * The tester or locator call made on the Wrapper: renamed, or, for a row call, made on
             * {@code row(index)} with the remaining arguments ({@code getCellText(r, c)} → {@code row(r).getCellText(c)}).
             */
            private J.MethodInvocation operation(J.MethodInvocation m, Expression wrapperValue, Browserless.Wrapper wrapper,
                    J.MethodInvocation original, String unified) {
                JavaType returned = m.getMethodType() == null ? null : m.getMethodType().getReturnType();
                if (!Browserless.isRowCall(wrapper, original)) {
                    return unifiedCall(m.getPadding().withSelect(JRightPadded.build(wrapperValue)), wrapper.fqn(), unified,
                            returned);
                }
                String rowType = wrapper.fqn() + "$UnifiedGridRow";
                JavaType.Method rowMethod = new JavaType.Method(null, 1L, JavaType.ShallowClass.build(wrapper.fqn()), "row",
                        JavaType.ShallowClass.build(rowType), List.of("index"), List.of(JavaType.Primitive.Int), null, null,
                        null, null);
                J.MethodInvocation row = new J.MethodInvocation(Tree.randomId(), wrapperValue.getPrefix(), Markers.EMPTY,
                        JRightPadded.build(wrapperValue.withPrefix(Space.EMPTY)), null,
                        new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "row", rowMethod, null),
                        org.openrewrite.java.tree.JContainer.build(Space.EMPTY,
                                List.of(JRightPadded.build(m.getArguments().get(0).withPrefix(Space.EMPTY))), Markers.EMPTY),
                        rowMethod);
                List<Expression> rest = m.getArguments().subList(1, m.getArguments().size()).stream()
                        .map(a -> a == m.getArguments().get(1) ? a.withPrefix(Space.EMPTY) : a).toList();
                J.MethodInvocation call = m.getPadding().withSelect(JRightPadded.build(row))
                        .withArguments(rest.isEmpty() ? List.of(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)) : rest);
                JavaType.Method type = new JavaType.Method(null, 1L, JavaType.ShallowClass.build(rowType), unified,
                        returned instanceof JavaType.Primitive || TypeUtils.isString(returned) ? returned : JavaType.Primitive.Void,
                        rest.stream().map(a -> "p").toList(),
                        rest.stream().map(a -> a.getType() == null ? (JavaType) JavaType.ShallowClass.build("java.lang.Object") : a.getType()).toList(),
                        null, null, null, null);
                return call.withMethodType(type).withName(call.getName().withSimpleName(unified).withType(type));
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

    /** {@code query.single()}, typed as returning the Wrapper. */
    private static J.MethodInvocation single(Expression query, Browserless.Wrapper wrapper) {
        JavaType.Method type = new JavaType.Method(null, 1L, JavaType.ShallowClass.build(QUERY), "single",
                JavaType.ShallowClass.build(wrapper.fqn()), List.of(), List.of(), null, null, null, null);
        return new J.MethodInvocation(Tree.randomId(), query.getPrefix(), Markers.EMPTY,
                JRightPadded.build(query.withPrefix(Space.EMPTY)), null,
                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "single", type, null),
                org.openrewrite.java.tree.JContainer.build(Space.EMPTY,
                        List.of(JRightPadded.<Expression>build(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY))),
                        Markers.EMPTY),
                type);
    }

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
