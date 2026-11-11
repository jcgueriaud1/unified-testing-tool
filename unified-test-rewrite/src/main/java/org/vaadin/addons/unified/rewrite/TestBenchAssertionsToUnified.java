package org.vaadin.addons.unified.rewrite;

import java.util.Map;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

/**
 * Turns {@code assertEquals(expected, element.getValue())} and {@code assertEquals(expected, element.getText())}
 * on an element that has a Wrapper into a retrying {@code assertThat(element).hasValue(expected)} or
 * {@code hasText(expected)}. Other assertions are left as they are.
 */
public class TestBenchAssertionsToUnified extends Recipe {

    static final String UNIFIED_ASSERTIONS = "com.vaadin.testing.unified.assertion.UnifiedAssertions";
    private static final MethodMatcher ASSERT_EQUALS =
            new MethodMatcher("org.junit.jupiter.api.Assertions assertEquals(Object, Object)");
    /** Getter on the element → the Unified assertion that retries it. */
    private static final Map<String, String> ASSERTION_BY_GETTER = Map.of("getValue", "hasValue", "getText", "hasText");

    /** Whether the tree is a {@code UnifiedAssertions.assertThat(..)} call, such as this recipe writes. */
    static boolean isUnifiedAssertion(Object tree) {
        return tree instanceof J.MethodInvocation call && call.getSimpleName().equals("assertThat")
                && call.getMethodType() != null
                && call.getMethodType().getDeclaringType().getFullyQualifiedName().equals(UNIFIED_ASSERTIONS);
    }

    @Override
    public String getDisplayName() {
        return "Migrate JUnit assertions on TestBench elements to retrying Unified assertions";
    }

    @Override
    public String getDescription() {
        return "Turns `assertEquals(expected, element.getValue())` and `getText()` into `assertThat(element).hasValue(expected)` "
                + "and `hasText(expected)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!ASSERT_EQUALS.matches(m) || !(m.getArguments().get(1) instanceof J.MethodInvocation getter)
                        || !getter.getArguments().stream().allMatch(a -> a instanceof J.Empty)
                        || getter.getSelect() == null) {
                    return m;
                }
                Wrappers.Wrapper wrapper = getter.getSelect() instanceof J.Identifier local
                        ? Locals.wrapperOf(getCursor(), local)
                        : Wrappers.ofLookup(getter.getSelect());
                if (wrapper == null || !wrapper.methods().contains(getter.getSimpleName())) {
                    return m;
                }
                String assertion = ASSERTION_BY_GETTER.get(getter.getSimpleName());
                if (assertion == null) {
                    return m;
                }
                maybeRemoveImport("org.junit.jupiter.api.Assertions");
                maybeRemoveImport("org.junit.jupiter.api.Assertions.assertEquals");
                maybeAddImport(UNIFIED_ASSERTIONS, "assertThat", false);
                return JavaTemplate.builder("assertThat(#{any(java.lang.Object)})." + assertion + "(#{any(java.lang.String)})")
                        .staticImports(UNIFIED_ASSERTIONS + ".assertThat")
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), getter.getSelect(), m.getArguments().get(0));
            }
        };
    }
}
