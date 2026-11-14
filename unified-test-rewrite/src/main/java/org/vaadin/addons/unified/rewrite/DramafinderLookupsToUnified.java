package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

/**
 * Dramafinder element code to Wrappers (ADR 0006): a lookup on the test's page ({@code XElement.getByLabel(page, ..)},
 * {@code find(page, XElement.class)...single()}) becomes {@code page.find(UnifiedX.class)...single()}, element
 * actions become the Wrapper's, and element assertions ({@code assertValue}, {@code assertOpen}, ...) become retrying
 * {@code UnifiedAssertions}. A lookup converts only when every call on what it finds does, directly or through a
 * local. Waits Auto-Wait makes unnecessary ({@code AutoWait.waitForVaadinIdle}, {@code waitForLoadState}) go.
 */
public class DramafinderLookupsToUnified extends Recipe {

    private static final String LOCALS = "unified.dramafinder.locals";
    private static final String ASSERT_THAT = TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS + ".assertThat";

    @Override
    public String getDisplayName() {
        return "Migrate Dramafinder element code to Wrappers";
    }

    @Override
    public String getDescription() {
        return "Turns Dramafinder lookups on the test's page into Unified Page lookups, and their actions and "
                + "assertions into the Wrapper's and retrying Unified assertions.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (method.getBody() == null || !Pages.hasPageParameter(method)) {
                    return method;
                }
                var markers = MigrationMarkers.collect(getCursor());
                Map<String, DramafinderWrappers.Wrapper> locals = new HashMap<>();
                getCursor().putMessage(LOCALS, locals);
                J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, ctx);
                m = m.withBody(m.getBody().withStatements(m.getBody().getStatements().stream()
                        .filter(s -> !isUnneededWait(s) && !DramafinderGrids.isDropped(s, n -> isGrid(locals, n))).toList()));
                return MigrationMarkers.write(m, markers);
            }

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                Map<String, DramafinderWrappers.Wrapper> locals = getCursor().getNearestMessage(LOCALS);
                J.MethodDeclaration method = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (locals != null && multiVariable.getVariables().size() == 1 && multiVariable.getTypeExpression() != null
                        && DramafinderGrids.isFindRow(multiVariable.getVariables().get(0).getInitializer(), n -> isGrid(locals, n))
                        && DramafinderGrids.rowUsesConvert(method, multiVariable.getVariables().get(0).getSimpleName())) {
                    J.VariableDeclarations v = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
                    locals.put(v.getVariables().get(0).getSimpleName(), DramafinderWrappers.ROW);
                    return retyped(v, DramafinderWrappers.ROW);
                }
                DramafinderWrappers.Lookup lookup = locals == null || multiVariable.getVariables().size() != 1
                        || multiVariable.getTypeExpression() == null
                        ? null : DramafinderWrappers.ofLookup(multiVariable.getVariables().get(0).getInitializer());
                String name = lookup == null ? null : multiVariable.getVariables().get(0).getSimpleName();
                if (lookup == null || !(isGridWrapper(lookup.wrapper()) ? DramafinderGrids.gridUsesConvert(method, name)
                        : usedOnlyThrough(method, name, lookup.wrapper()))) {
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
                J.VariableDeclarations v = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
                locals.put(v.getVariables().get(0).getSimpleName(), lookup.wrapper());
                DramafinderWrappers.Wrapper wrapper = lookup.wrapper();
                maybeAddImport(wrapper.fqn());
                maybeRemoveImport(DramafinderWrappers.elementOf(wrapper.fqn()));
                if (lookup.marker() != null) {
                    MigrationMarkers.add(getCursor(), lookup.marker());
                }
                List<Object> parameters = new ArrayList<>();
                String code = DramafinderWrappers.code(lookup, parameters);
                J.VariableDeclarations.NamedVariable variable = v.getVariables().get(0);
                Expression initializer = variable.getInitializer();
                Expression converted = typed(template(code, wrapper).apply(
                        new Cursor(getCursor(), initializer), initializer.getCoordinates().replace(), parameters.toArray()), wrapper);
                return retyped(v.withVariables(List.of(variable.withInitializer(converted.withPrefix(initializer.getPrefix()))
                        .withName(variable.getName().withFieldType(null)))), wrapper);
            }

            /** The declaration with the Wrapper's type. */
            private J.VariableDeclarations retyped(J.VariableDeclarations v, DramafinderWrappers.Wrapper wrapper) {
                maybeAddImport(wrapper.importFqn());
                JavaType.ShallowClass type = JavaType.ShallowClass.build(wrapper.fqn());
                J.VariableDeclarations.NamedVariable variable = v.getVariables().get(0);
                return v.withTypeExpression(new J.Identifier(Tree.randomId(), v.getTypeExpression().getPrefix(),
                                Markers.EMPTY, emptyList(), wrapper.simpleName(), type, null))
                        .withVariables(List.of(variable.withType(type).withName(variable.getName().withType(type))));
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                Map<String, DramafinderWrappers.Wrapper> locals = getCursor().getNearestMessage(LOCALS);
                DramafinderWrappers.Wrapper wrapper = locals == null ? null : locals.get(identifier.getSimpleName());
                if (wrapper == null || identifier.getFieldType() != null && identifier.getFieldType().getOwner() != null
                        && !(identifier.getFieldType().getOwner() instanceof JavaType.Method)) {
                    return identifier;
                }
                return identifier.withType(JavaType.ShallowClass.build(wrapper.fqn())); // a use of a converted local
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                Map<String, DramafinderWrappers.Wrapper> locals = getCursor().getNearestMessage(LOCALS);
                if (locals == null || !(j instanceof J.MethodInvocation m) || method.getSelect() == null) {
                    return j;
                }
                boolean statement = getCursor().getParentTreeCursor().getValue() instanceof J.Block;
                J grid = gridCall(method, m, locals, statement);
                if (grid != null) {
                    return grid;
                }
                // a call on a local that now holds a Wrapper
                if (method.getSelect() instanceof J.Identifier id && locals.containsKey(id.getSimpleName())) {
                    DramafinderWrappers.Wrapper wrapper = locals.get(id.getSimpleName());
                    String assertion = wrapper.assertions().get(method.getSimpleName());
                    if (assertion != null && statement) {
                        return typed(assertion(m, "#{any()}", assertion, List.of(m.getSelect()), wrapper), wrapper);
                    }
                    String renamed = wrapper.methods().get(method.getSimpleName());
                    return renamed == null ? m : onWrapper(m.withName(m.getName().withSimpleName(renamed)), wrapper);
                }
                // a call on a lookup
                DramafinderWrappers.Lookup lookup = DramafinderWrappers.ofLookup(method.getSelect());
                if (lookup == null || !supportedCall(lookup.wrapper(), method, statement)) {
                    return m;
                }
                DramafinderWrappers.Wrapper wrapper = lookup.wrapper();
                maybeAddImport(wrapper.fqn());
                maybeRemoveImport(DramafinderWrappers.elementOf(wrapper.fqn()));
                if (lookup.marker() != null) {
                    MigrationMarkers.add(getCursor(), lookup.marker());
                }
                List<Object> parameters = new ArrayList<>();
                String code = DramafinderWrappers.code(lookup, parameters);
                String assertion = wrapper.assertions().get(method.getSimpleName());
                if (assertion != null) {
                    return typed(assertion(m, code, assertion, parameters, wrapper), wrapper);
                }
                List<Expression> arguments = arguments(m);
                parameters.addAll(arguments);
                return typed(template(code + "." + wrapper.methods().get(method.getSimpleName()) + "("
                                + String.join(", ", java.util.Collections.nCopies(arguments.size(), "#{any()}")) + ")", wrapper)
                        .<J>apply(getCursor(), m.getCoordinates().replace(), parameters.toArray()).withId(m.getId()), wrapper);
            }

            /** The grid and row patterns of {@link DramafinderGrids}, converted; {@code null} for any other call. */
            private J gridCall(J.MethodInvocation original, J.MethodInvocation m, Map<String, DramafinderWrappers.Wrapper> locals,
                    boolean statement) {
                DramafinderWrappers.Wrapper grid = DramafinderWrappers.forElement(JavaType.ShallowClass.build(
                        "org.vaadin.addons.dramafinder.element.GridElement"));
                if (DramafinderGrids.isRowIndexLookup(original, n -> isGrid(locals, n))) {
                    J.MethodInvocation indexes = (J.MethodInvocation) m.getSelect();
                    Expression column = indexes.getArguments().get(0);
                    // Dramafinder matches one column by its index; a Unified row query names a header, or any column
                    MigrationMarkers.add(getCursor(), "was findRowIndexesWithColumnText(" + column.printTrimmed(getCursor())
                            + ", ..).get(0): any column matches now; single() fails when several rows match");
                    return typed(template("#{any()}.rows().withCellText(#{any(java.lang.String)}).single().getIndex()", grid)
                            .<J>apply(getCursor(), m.getCoordinates().replace(), indexes.getSelect(), indexes.getArguments().get(1))
                            .withId(m.getId()), DramafinderWrappers.ROW);
                }
                if (DramafinderGrids.isFindRow(original, n -> isGrid(locals, n))) {
                    J.MethodInvocation findRow = (J.MethodInvocation) m.getSelect();
                    return typed(template("#{any()}.row(#{any(int)})", grid).<J>apply(getCursor(), m.getCoordinates().replace(),
                            findRow.getSelect(), findRow.getArguments().get(0)).withId(m.getId()), DramafinderWrappers.ROW);
                }
                if (original.getSelect() != null && DramafinderGrids.isGetCell(original.getSelect(),
                        n -> locals.get(n) == DramafinderWrappers.ROW) && DramafinderGrids.cellCallConverts(original, statement)) {
                    J.MethodInvocation cell = (J.MethodInvocation) m.getSelect();
                    Expression row = cell.getSelect();
                    Expression column = cell.getArguments().get(0);
                    return switch (original.getSimpleName()) {
                        case "click" -> {
                            MigrationMarkers.add(getCursor(), "was getCell(" + column.printTrimmed(getCursor())
                                    + ").click(); a row's click() clicks the row, not that cell");
                            yield typed(template("#{any()}.click()", grid).<J>apply(getCursor(), m.getCoordinates().replace(), row)
                                    .withId(m.getId()), DramafinderWrappers.ROW);
                        }
                        case "getText" -> typed(template("#{any()}.getCellText(#{any()})", grid).<J>apply(getCursor(),
                                m.getCoordinates().replace(), row, column).withId(m.getId()), DramafinderWrappers.ROW);
                        default -> {
                            maybeAddImport(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS, "assertThat", false);
                            yield typed(JavaTemplate.builder("assertThat(#{any()}).hasCellText(#{any(java.lang.String)}, #{any()})")
                                    .staticImports(ASSERT_THAT)
                                    .javaParser(Stubs.parser())
                                    .build()
                                    .<J>apply(getCursor(), m.getCoordinates().replace(), row, column, m.getArguments().get(0))
                                    .withId(m.getId()), DramafinderWrappers.ROW);
                        }
                    };
                }
                return null;
            }

            /** {@code assertThat(<wrapper code>).<assertion>(..)} with the element assertion's arguments. */
            private J assertion(J.MethodInvocation m, String wrapperCode, String assertion, List<Object> parameters,
                    DramafinderWrappers.Wrapper wrapper) {
                maybeAddImport(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS, "assertThat", false);
                List<Object> all = new ArrayList<>(parameters);
                List<Expression> arguments = arguments(m);
                all.addAll(arguments);
                return JavaTemplate.builder("assertThat(" + wrapperCode + ")." + assertion + "("
                                + String.join(", ", java.util.Collections.nCopies(arguments.size(), "#{any()}")) + ")")
                        .staticImports(ASSERT_THAT)
                        .imports(wrapper.importFqn())
                        .javaParser(Stubs.parser(Stubs.interfaceStub(wrapper.importFqn())))
                        .build()
                        .<J>apply(getCursor(), m.getCoordinates().replace(), all.toArray()).withId(m.getId());
            }

            private JavaTemplate template(String code, DramafinderWrappers.Wrapper wrapper) {
                return JavaTemplate.builder(code)
                        .imports(wrapper.importFqn())
                        .javaParser(Stubs.parser(Stubs.interfaceStub(wrapper.importFqn())))
                        .build();
            }
        };
    }

    private static final String QUERY = "com.vaadin.testing.unified.query.Query";

    /**
     * The tree with a Unified method type on every call the templates wrote untyped (the stubs don't know the
     * Wrappers' queries), so later recipes find no Dramafinder type in it and the tree stays valid.
     */
    @SuppressWarnings("unchecked")
    static <T extends J> T typed(T tree, DramafinderWrappers.Wrapper wrapper) {
        return (T) new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                J.MethodInvocation m = super.visitMethodInvocation(method, p);
                if (m.getMethodType() != null && !(m.getMethodType().getDeclaringType() instanceof JavaType.Unknown)) {
                    return m;
                }
                JavaType.FullyQualified declaring = m.getSelect() != null
                        && m.getSelect().getType() instanceof JavaType.FullyQualified fq && !(fq instanceof JavaType.Unknown)
                        ? fq : JavaType.ShallowClass.build(QUERY);
                String name = m.getSimpleName();
                JavaType returned = switch (name) {
                    case "single", "get" -> JavaType.ShallowClass.build(wrapper.fqn());
                    case "all" -> JavaType.ShallowClass.build("java.util.List");
                    default -> declaring.getFullyQualifiedName().equals(QUERY)
                            ? JavaType.ShallowClass.build(QUERY) : JavaType.Primitive.Void;
                };
                return withType(m, declaring, returned);
            }
        }.visitNonNull(tree, 0);
    }

    /** A call on a local that now holds the Wrapper: typed on the Wrapper, not on Dramafinder's element. */
    private static J.MethodInvocation onWrapper(J.MethodInvocation m, DramafinderWrappers.Wrapper wrapper) {
        JavaType returned = m.getMethodType() == null ? JavaType.Primitive.Void : m.getMethodType().getReturnType();
        return withType(m, JavaType.ShallowClass.build(wrapper.fqn()), returned);
    }

    private static J.MethodInvocation withType(J.MethodInvocation m, JavaType.FullyQualified declaring, JavaType returned) {
        List<JavaType> parameterTypes = arguments(m).stream()
                .map(a -> a.getType() == null ? (JavaType) JavaType.ShallowClass.build("java.lang.Object") : a.getType()).toList();
        List<String> parameterNames = java.util.stream.IntStream.range(0, parameterTypes.size()).mapToObj(i -> "arg" + i).toList();
        JavaType.Method type = new JavaType.Method(null, 1L, declaring, m.getSimpleName(), returned, parameterNames,
                parameterTypes, null, null, null, null);
        J.MethodInvocation typed = m.withMethodType(type);
        return typed.withName(typed.getName().withType(type));
    }

    private static boolean isGridWrapper(DramafinderWrappers.Wrapper wrapper) {
        return wrapper.fqn().equals("com.vaadin.testing.unified.component.UnifiedGrid");
    }

    private static boolean isGrid(Map<String, DramafinderWrappers.Wrapper> locals, String name) {
        DramafinderWrappers.Wrapper wrapper = locals.get(name);
        return wrapper != null && isGridWrapper(wrapper);
    }

    private static List<Expression> arguments(J.MethodInvocation m) {
        return m.getArguments().stream().filter(a -> !(a instanceof J.Empty)).toList();
    }

    /** A call the Wrapper has; an assertion only as a statement of its own. */
    private static boolean supportedCall(DramafinderWrappers.Wrapper wrapper, J.MethodInvocation call, boolean statement) {
        return wrapper.methods().containsKey(call.getSimpleName())
                || statement && wrapper.assertions().containsKey(call.getSimpleName());
    }

    /** Whether every use of the local is a call the Wrapper has. */
    private static boolean usedOnlyThrough(J.MethodDeclaration method, String name, DramafinderWrappers.Wrapper wrapper) {
        AtomicBoolean other = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                Cursor parentCursor = getCursor().getParentTreeCursor();
                Object parent = parentCursor.getValue();
                if (identifier.getSimpleName().equals(name)
                        && !(parent instanceof J.VariableDeclarations.NamedVariable)
                        && !(parent instanceof J.MethodInvocation call && call.getSelect() == identifier
                                && supportedCall(wrapper, call, parentCursor.getParentTreeCursor().getValue() instanceof J.Block))) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(method.getBody(), other);
        return !other.get();
    }

    /** {@code AutoWait.waitForVaadinIdle(page)} or {@code page.waitForLoadState()}: Auto-Wait does it. */
    private static boolean isUnneededWait(Statement s) {
        if (!(s instanceof J.MethodInvocation call) || call.getMethodType() == null) {
            return false;
        }
        String declaring = call.getMethodType().getDeclaringType().getFullyQualifiedName();
        return declaring.equals("org.vaadin.addons.dramafinder.AutoWait") && call.getSimpleName().equals("waitForVaadinIdle")
                || declaring.equals(Dramafinder.PAGE) && call.getSimpleName().equals("waitForLoadState")
                        && call.getSelect() != null && Dramafinder.isPageField(call.getSelect());
    }
}
