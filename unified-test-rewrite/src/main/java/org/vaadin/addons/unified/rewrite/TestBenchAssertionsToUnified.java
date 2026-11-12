package org.vaadin.addons.unified.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
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
    private static final MethodMatcher ASSERT_TRUE = new MethodMatcher("org.junit.jupiter.api.Assertions assertTrue(boolean)");
    private static final MethodMatcher ASSERT_FALSE = new MethodMatcher("org.junit.jupiter.api.Assertions assertFalse(boolean)");
    /** Boolean getter on the element → the Unified assertions for true and for false. */
    private static final Map<String, String[]> ASSERTIONS_BY_CHECK = Map.of(
            "isChecked", new String[] {"isChecked", "isNotChecked"},
            "isEnabled", new String[] {"isEnabled", "isDisabled"});
    /** List getter on the element → the Unified assertion that retries it, for an expected {@code List.of(..)}. */
    private static final Map<String, String> ASSERTION_BY_LIST_GETTER = Map.of(
            "getSelectedTexts", "hasSelectedItems", "getOptions", "hasItems");
    private static final MethodMatcher LIST_OF = new MethodMatcher("java.util.List of(..)");
    private static final MethodMatcher ARRAYS_AS_LIST = new MethodMatcher("java.util.Arrays asList(..)");
    /** Getter on the element → the Unified assertion that retries it. */
    private static final Map<String, String> ASSERTION_BY_GETTER = Map.of("getValue", "hasValue", "getText", "hasText",
            "getSelectedText", "hasSelectedItem", "getDate", "hasValue");

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
                boolean asserted = ASSERT_TRUE.matches(m);
                if ((asserted || ASSERT_FALSE.matches(m)) && m.getArguments().get(0) instanceof J.MethodInvocation check
                        && ASSERTIONS_BY_CHECK.containsKey(check.getSimpleName()) && check.getSelect() != null
                        && check.getArguments().stream().allMatch(a -> a instanceof J.Empty)) {
                    Wrappers.Wrapper wrapper = wrapperOf(check.getSelect());
                    if (wrapper == null || !wrapper.has(check.getSimpleName())) {
                        return m;
                    }
                    String assertion = ASSERTIONS_BY_CHECK.get(check.getSimpleName())[asserted ? 0 : 1];
                    imports();
                    return JavaTemplate.builder("assertThat(#{any(java.lang.Object)})." + assertion + "()")
                            .staticImports(UNIFIED_ASSERTIONS + ".assertThat")
                            .javaParser(Stubs.parser())
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace(), check.getSelect());
                }
                if (!ASSERT_EQUALS.matches(m) || !(m.getArguments().get(1) instanceof J.MethodInvocation getter)
                        || !getter.getArguments().stream().allMatch(a -> a instanceof J.Empty)
                        || getter.getSelect() == null) {
                    return m;
                }
                Wrappers.Wrapper wrapper = wrapperOf(getter.getSelect());
                if (wrapper == null || !wrapper.has(getter.getSimpleName())) {
                    return m;
                }
                String listAssertion = ASSERTION_BY_LIST_GETTER.get(getter.getSimpleName());
                if (listAssertion != null && m.getArguments().get(0) instanceof J.MethodInvocation list
                        && (LIST_OF.matches(list) || ARRAYS_AS_LIST.matches(list))) {
                    List<Expression> items = list.getArguments().stream().filter(a -> !(a instanceof J.Empty)).toList();
                    String parameters = String.join(", ", java.util.Collections.nCopies(items.size(), "#{any(java.lang.String)}"));
                    List<Object> values = new ArrayList<>();
                    values.add(getter.getSelect());
                    values.addAll(items);
                    imports();
                    maybeRemoveImport("java.util.List");
                    maybeRemoveImport("java.util.Arrays");
                    return JavaTemplate.builder("assertThat(#{any(java.lang.Object)})." + listAssertion + "(" + parameters + ")")
                            .staticImports(UNIFIED_ASSERTIONS + ".assertThat")
                            .javaParser(Stubs.parser())
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace(), values.toArray());
                }
                String assertion = ASSERTION_BY_GETTER.get(getter.getSimpleName());
                if (assertion == null) {
                    return m;
                }
                imports();
                return JavaTemplate.builder("assertThat(#{any(java.lang.Object)})." + assertion + "(#{any(java.lang.String)})")
                        .staticImports(UNIFIED_ASSERTIONS + ".assertThat")
                        .javaParser(Stubs.parser())
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), getter.getSelect(), m.getArguments().get(0));
            }

            private Wrappers.Wrapper wrapperOf(Expression element) {
                return element instanceof J.Identifier local
                        ? Locals.wrapperOf(getCursor(), local)
                        : Wrappers.ofLookup(element);
            }

            private void imports() {
                maybeRemoveImport("org.junit.jupiter.api.Assertions");
                maybeRemoveImport("org.junit.jupiter.api.Assertions.assertEquals");
                maybeRemoveImport("org.junit.jupiter.api.Assertions.assertTrue");
                maybeRemoveImport("org.junit.jupiter.api.Assertions.assertFalse");
                maybeAddImport(UNIFIED_ASSERTIONS, "assertThat", false);
            }
        };
    }
}
