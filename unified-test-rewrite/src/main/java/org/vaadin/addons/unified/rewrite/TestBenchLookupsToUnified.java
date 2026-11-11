package org.vaadin.addons.unified.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
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
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!DOLLAR.matches(m) || m.getSelect() != null || !hasPageParameter()
                        || !(m.getArguments().get(0) instanceof J.FieldAccess classLiteral)
                        || !(classLiteral.getTarget().getType() instanceof JavaType.FullyQualified element)) {
                    return m;
                }
                String wrapper = Wrappers.forElement(element.getFullyQualifiedName());
                if (wrapper == null) {
                    return m;
                }
                maybeAddImport(wrapper);
                maybeRemoveImport(element.getFullyQualifiedName());
                return JavaTemplate.builder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}.find("
                                + wrapper.substring(wrapper.lastIndexOf('.') + 1) + ".class)")
                        .imports(wrapper)
                        .javaParser(Stubs.parser(Stubs.interfaceStub(wrapper)))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), Stubs.page());
            }

            private boolean hasPageParameter() {
                J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return enclosing != null && Pages.hasPageParameter(enclosing);
            }
        };
    }
}
