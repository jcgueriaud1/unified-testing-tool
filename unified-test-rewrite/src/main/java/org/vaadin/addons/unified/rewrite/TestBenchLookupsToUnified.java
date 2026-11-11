package org.vaadin.addons.unified.rewrite;

import org.openrewrite.Cursor;
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
                Wrappers.Wrapper wrapper = Wrappers.forElement(element.getFullyQualifiedName());
                if (wrapper == null || !onlyUsesWhatTheWrapperHas(wrapper)) {
                    return m;
                }
                maybeAddImport(wrapper.fqn());
                maybeRemoveImport(element.getFullyQualifiedName());
                return JavaTemplate.builder("#{any(" + TestBenchSetupToUnified.UNIFIED_PAGE + ")}.find("
                                + wrapper.simpleName() + ".class)")
                        .imports(wrapper.fqn())
                        .javaParser(Stubs.parser(Stubs.interfaceStub(wrapper.fqn())))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), Stubs.page());
            }

            /**
             * Whether the chain on this {@code $(..)} only uses the Wrapper's filters, then {@code single()}, then at
             * most one of the Wrapper's methods, or ends in a Unified assertion or a statement of its own.
             */
            private boolean onlyUsesWhatTheWrapperHas(Wrappers.Wrapper wrapper) {
                Cursor c = getCursor();
                boolean found = false;
                while (true) {
                    Cursor parent = c.getParentTreeCursor();
                    if (!(parent.getValue() instanceof J.MethodInvocation call) || call.getSelect() != c.getValue()) {
                        return found && (parent.getValue() instanceof J.Block || isUnifiedAssertion(parent.getValue()));
                    }
                    String name = call.getSimpleName();
                    if (!found && name.equals("single")) {
                        found = true;
                    } else if (!found && wrapper.filters().contains(name)) {
                        // a filter before the terminal
                    } else {
                        return found && wrapper.methods().contains(name);
                    }
                    c = parent;
                }
            }

            private boolean isUnifiedAssertion(Object tree) {
                return tree instanceof J.MethodInvocation call && call.getSimpleName().equals("assertThat")
                        && call.getMethodType() != null
                        && call.getMethodType().getDeclaringType().getFullyQualifiedName()
                                .equals(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS);
            }

            private boolean hasPageParameter() {
                J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return enclosing != null && Pages.hasPageParameter(enclosing);
            }
        };
    }
}
