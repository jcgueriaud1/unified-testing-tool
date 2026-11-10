package com.vaadin.testing.unified.spring;

import com.vaadin.testing.unified.TestingTool;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * Registered by {@link UnifiedSpringBootTest}: fails a {@code @Transactional} test on a browser Testing Tool.
 * The test's transaction is rolled back after the test, but in a browser the views run on the server's
 * request threads, outside it: they don't see what the test wrote and don't roll back what they write. On
 * Browserless the same test passes, so it would pass on one tool and fail (or leak data) on another.
 * <p>
 * A condition, not a before-each callback: JUnit evaluates it before Spring starts the transaction, and
 * reports the exception as the test's failure.
 */
final class TransactionalCheck implements ExecutionCondition {

    private static final List<String> TRANSACTIONAL = List.of(
            "org.springframework.transaction.annotation.Transactional", "jakarta.transaction.Transactional");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        TestingTool tool = context.getConfigurationParameter("vaadin.testing.tool").filter(v -> !v.isBlank())
                .map(TestingTool::parse).orElse(TestingTool.BROWSERLESS);
        if (!tool.hasRealBrowser() || context.getTestClass().isEmpty()) {
            return ConditionEvaluationResult.enabled("no transaction check needed");
        }
        List<AnnotatedElement> elements = new ArrayList<>();
        context.getTestMethod().ifPresent(elements::add);
        for (Class<?> c = context.getRequiredTestClass(); c != null; c = c.getEnclosingClass()) {
            elements.add(c);
        }
        for (AnnotatedElement element : elements) {
            for (String annotation : TRANSACTIONAL) {
                AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(element,
                        annotation, true, false);
                if (attributes == null) {
                    continue;
                }
                if (startsNoTransaction(attributes)) {
                    break; // the nearest declaration wins
                }
                throw new IllegalStateException("@Transactional on " + describe(element) + " doesn't work on " + tool
                        + ": the views run on the server's request threads, outside the test's transaction, so"
                        + " they don't see data the test wrote and what they write isn't rolled back. It works on"
                        + " Browserless only. Commit the test data and reset the database after each test instead"
                        + " (see \"Test data\" in the spec's 02-test-setup).");
            }
        }
        return ConditionEvaluationResult.enabled("not @Transactional");
    }

    /** {@code propagation = NOT_SUPPORTED} or {@code NEVER}: the test runs without a transaction. */
    private static boolean startsNoTransaction(AnnotationAttributes attributes) {
        Object propagation = attributes.get(attributes.containsKey("propagation") ? "propagation" : "value");
        String name = propagation instanceof Enum<?> e ? e.name() : String.valueOf(propagation);
        return name.equals("NOT_SUPPORTED") || name.equals("NEVER");
    }

    private static String describe(AnnotatedElement element) {
        return element instanceof Class<?> c ? c.getSimpleName()
                : ((java.lang.reflect.Method) element).getDeclaringClass().getSimpleName() + "."
                        + ((java.lang.reflect.Method) element).getName();
    }
}
