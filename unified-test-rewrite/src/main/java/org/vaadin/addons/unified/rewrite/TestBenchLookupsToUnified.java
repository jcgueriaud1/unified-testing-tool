package org.vaadin.addons.unified.rewrite;

import static java.util.Collections.emptyList;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Turns {@code $(XElement.class)} into {@code page.find(UnifiedX.class)} for elements that have a Wrapper,
 * in test methods that have the {@code UnifiedPage page} parameter. The filters and terminals after it
 * ({@code withLabel}, {@code withText}, {@code withId}, {@code single}) keep their names.
 */
public class TestBenchLookupsToUnified extends Recipe {

    private static final MethodMatcher DOLLAR = new MethodMatcher("com.vaadin.testbench.HasElementQuery $(java.lang.Class)", true);

    private static final Set<String> FIRST = Set.of("first", "waitForFirst");

    @Override
    public String getDisplayName() {
        return "Migrate TestBench element lookups to Unified Page lookups";
    }

    @Override
    public String getDescription() {
        return "Turns `$(XElement.class)` into `page.find(UnifiedX.class)` for elements that have a Wrapper.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                var markers = MigrationMarkers.collect(getCursor());
                return MigrationMarkers.write(super.visitMethodDeclaration(method, ctx), markers);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.MethodDeclaration method = getCursor().firstEnclosing(J.MethodDeclaration.class);
                Wrappers.Wrapper wrapper = method == null || multiVariable.getVariables().size() != 1
                        || multiVariable.getTypeExpression() == null || !Pages.hasPageParameter(method)
                        ? null : Locals.wrapperOf(method, multiVariable.getVariables().get(0));
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                if (wrapper == null) {
                    return v;
                }
                maybeAddImport(wrapper.fqn());
                return v.withTypeExpression(new J.Identifier(Tree.randomId(), v.getTypeExpression().getPrefix(),
                        Markers.EMPTY, emptyList(), wrapper.simpleName(), JavaType.ShallowClass.build(wrapper.fqn()), null));
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (FIRST.contains(m.getSimpleName()) && hasPageParameter()) {
                    Wrappers.Wrapper wrapper = Wrappers.ofLookup(method);
                    if (wrapper != null && Chains.supported(getCursor(), true, wrapper)
                            && MigrationMarkers.add(getCursor(),
                                    "was " + m.getSimpleName() + "(); single() fails when several components match")) {
                        return m.withName(m.getName().withSimpleName("single"));
                    }
                    return m;
                }
                if (!DOLLAR.matches(method) || !hasPageParameter()) {
                    return m;
                }
                Wrappers.Wrapper wrapper = Chains.ofDollar(method);
                if (wrapper == null || !Chains.supported(getCursor(), false, wrapper)) {
                    return m;
                }
                // $(..) on the page, or on a container that becomes a Unified Scope
                String scope = TestBenchSetupToUnified.UNIFIED_PAGE;
                Object receiver = Stubs.page();
                if (method.getSelect() instanceof J.MethodInvocation content && content.getSimpleName().equals("content")
                        && content.getSelect() != null && isTabSheet(content.getSelect())) {
                    scope = "com.vaadin.testing.unified.Scope"; // a TabSheet's content()
                    receiver = m.getSelect();
                } else if (method.getSelect() != null) {
                    Wrappers.Wrapper container = method.getSelect() instanceof J.Identifier local
                            ? Locals.wrapperOf(getCursor(), local)
                            : Wrappers.ofLookup(method.getSelect());
                    if (container == null || !Chains.SCOPES.contains(container.fqn())) {
                        return m;
                    }
                    scope = container.fqn();
                    receiver = m.getSelect();
                }
                String element = ((J.FieldAccess) method.getArguments().get(0)).getTarget().getType() instanceof JavaType.FullyQualified t
                        ? t.getFullyQualifiedName() : null;
                maybeAddImport(wrapper.fqn());
                maybeRemoveImport(element);
                return JavaTemplate.builder("#{any(" + scope + ")}.find(" + wrapper.simpleName() + ".class)")
                        .imports(wrapper.fqn())
                        .javaParser(Stubs.parser(Stubs.interfaceStub(wrapper.fqn())))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), receiver);
            }

            private boolean isTabSheet(org.openrewrite.java.tree.Expression tabSheet) {
                Wrappers.Wrapper w = tabSheet instanceof J.Identifier local
                        ? Locals.wrapperOf(getCursor(), local)
                        : Wrappers.ofLookup(tabSheet);
                return w != null && w.fqn().equals(TabSheets.TAB_SHEET);
            }

            private boolean hasPageParameter() {
                J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return enclosing != null && Pages.hasPageParameter(enclosing);
            }
        };
    }
}
