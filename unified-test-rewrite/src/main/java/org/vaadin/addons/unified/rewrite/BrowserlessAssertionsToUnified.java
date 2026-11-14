package org.vaadin.addons.unified.rewrite;

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
 * Turns JUnit assertions on what a Browserless lookup finds into retrying Unified assertions:
 * {@code assertEquals(expected, component.getValue())} and {@code assertEquals(expected, test(component).getText())}
 * become {@code assertThat(component).hasValue(expected)} and {@code hasText(expected)};
 * {@code assertTrue(component.isEnabled())} becomes {@code assertThat(component).isEnabled()}, and so on.
 * The lookup itself is converted afterwards. Other assertions are left as they are.
 */
public class BrowserlessAssertionsToUnified extends Recipe {

    private static final MethodMatcher ASSERT_EQUALS =
            new MethodMatcher("org.junit.jupiter.api.Assertions assertEquals(Object, Object)");
    private static final MethodMatcher ASSERT_TRUE = new MethodMatcher("org.junit.jupiter.api.Assertions assertTrue(boolean)");
    private static final MethodMatcher ASSERT_FALSE = new MethodMatcher("org.junit.jupiter.api.Assertions assertFalse(boolean)");
    /** Unified getter → the Unified assertion that retries it. */
    private static final Map<String, String> ASSERTION_BY_GETTER =
            Map.of("getValue", "hasValue", "getText", "hasText", "getHeaderTitle", "hasHeaderTitle");
    /** Unified boolean getter → the Unified assertions for true and for false ({@code null}: none). */
    private static final Map<String, String[]> ASSERTIONS_BY_CHECK = Map.of(
            "isChecked", new String[] {"isChecked", "isNotChecked"},
            "isEnabled", new String[] {"isEnabled", "isDisabled"},
            "isOpen", new String[] {"isOpen", "isClosed"});
    private static final String NOTIFICATION = "com.vaadin.testing.unified.component.UnifiedNotification";

    @Override
    public String getDisplayName() {
        return "Migrate JUnit assertions on Browserless lookups to retrying Unified assertions";
    }

    @Override
    public String getDescription() {
        return "Turns `assertEquals(expected, component.getValue())` and similar assertions on a component a Browserless "
                + "lookup found into `assertThat(component).hasValue(expected)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                boolean asserted = ASSERT_TRUE.matches(m);
                if (asserted || ASSERT_FALSE.matches(m)) {
                    Read read = read(m.getArguments().get(0));
                    String[] assertions = read == null ? null : ASSERTIONS_BY_CHECK.get(read.unifiedName());
                    if (assertions == null || !asserted && read.wrapper().fqn().equals(NOTIFICATION)) {
                        return m;
                    }
                    return assertThat(m, read.element(), assertions[asserted ? 0 : 1] + "()");
                }
                if (ASSERT_EQUALS.matches(m)) {
                    Read read = read(m.getArguments().get(1));
                    String assertion = read == null ? null : ASSERTION_BY_GETTER.get(read.unifiedName());
                    if (assertion == null) {
                        return m;
                    }
                    String parameter = assertion.equals("hasValue") ? "java.lang.Object" : "java.lang.String";
                    return assertThat(m, read.element(), assertion + "(#{any(" + parameter + ")})", m.getArguments().get(0));
                }
                return m;
            }

            private J.MethodInvocation assertThat(J.MethodInvocation m, Expression element, String assertion,
                    Object... arguments) {
                maybeRemoveImport("org.junit.jupiter.api.Assertions");
                maybeRemoveImport("org.junit.jupiter.api.Assertions.assertEquals");
                maybeRemoveImport("org.junit.jupiter.api.Assertions.assertTrue");
                maybeRemoveImport("org.junit.jupiter.api.Assertions.assertFalse");
                maybeAddImport(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS, "assertThat", false);
                Object[] all = new Object[arguments.length + 1];
                all[0] = element;
                System.arraycopy(arguments, 0, all, 1, arguments.length);
                return JavaTemplate.builder("assertThat(#{any(java.lang.Object)})." + assertion)
                        .staticImports(TestBenchAssertionsToUnified.UNIFIED_ASSERTIONS + ".assertThat")
                        .javaParser(Stubs.parser())
                        .build()
                        .<J.MethodInvocation>apply(getCursor(), m.getCoordinates().replace(), all)
                        .withId(m.getId()).withPrefix(m.getPrefix()); // the same statement: keeps its markers
            }

            /** {@code component.getter()} or {@code test(component).getter()} on a convertible component. */
            private Read read(Expression e) {
                if (!(e instanceof J.MethodInvocation getter) || !Browserless.noArguments(getter)
                        || getter.getSelect() == null) {
                    return null;
                }
                if (getter.getSelect() instanceof J.MethodInvocation test && Browserless.isTest(test)) {
                    Expression element = test.getArguments().get(0);
                    Browserless.Wrapper wrapper = wrapperOf(element);
                    String unified = wrapper == null ? null : wrapper.testerCalls().get(getter.getSimpleName());
                    return unified == null ? null : new Read(element, wrapper, unified);
                }
                // a locator's own getter: window.findNotification().getText()
                Browserless.Wrapper locator = Browserless.ofLocator(getter.getSelect());
                if (locator != null) {
                    String unified = locator.testerCalls().get(getter.getSimpleName());
                    return unified == null ? null : new Read(getter.getSelect(), locator, unified);
                }
                Browserless.Wrapper wrapper = wrapperOf(getter.getSelect());
                String unified = wrapper == null ? null : wrapper.getters().get(getter.getSimpleName());
                return unified == null ? null : new Read(getter.getSelect(), wrapper, unified);
            }

            private Browserless.Wrapper wrapperOf(Expression element) {
                return element instanceof J.Identifier local
                        ? Browserless.convertibleLocal(getCursor(), local)
                        : Browserless.ofLookup(element);
            }
        };
    }

    private record Read(Expression element, Browserless.Wrapper wrapper, String unifiedName) {
    }
}
