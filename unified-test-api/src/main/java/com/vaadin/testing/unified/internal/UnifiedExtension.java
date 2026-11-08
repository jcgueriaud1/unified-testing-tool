package com.vaadin.testing.unified.internal;

import com.vaadin.testing.unified.RequiresBrowser;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.spi.AdapterContext;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.TestingToolAdapter;
import java.lang.reflect.AnnotatedElement;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.ServiceLoader;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * JUnit 6 extension behind {@code @UnifiedUITest}: picks the run's Testing Tool adapter, skips tests whose
 * Capabilities it lacks, injects {@link UnifiedPage} and closes the session after each test.
 */
public final class UnifiedExtension implements ExecutionCondition, ParameterResolver, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UnifiedExtension.class);
    private static final String SESSION = "session";

    // ---------------------------------------------------------------- Capabilities

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        TestingTool tool = Settings.tool(context);
        Optional<AnnotatedElement> element = context.getElement();
        if (element.isEmpty()) {
            return ConditionEvaluationResult.enabled("no element");
        }
        Optional<RequiresTestingTool> requiredTool = find(context, RequiresTestingTool.class);
        if (requiredTool.isPresent() && Arrays.stream(requiredTool.get().value()).noneMatch(t -> t == tool)) {
            return ConditionEvaluationResult.disabled("Requires " + Arrays.toString(requiredTool.get().value())
                    + ", this run uses " + tool);
        }
        if (find(context, RequiresBrowser.class).isPresent() && !tool.hasRealBrowser()) {
            return ConditionEvaluationResult.disabled("Requires a real browser, this run uses " + tool);
        }
        return ConditionEvaluationResult.enabled("Runs on " + tool);
    }

    private static <A extends java.lang.annotation.Annotation> Optional<A> find(ExtensionContext context, Class<A> type) {
        Optional<A> onElement = context.getElement().flatMap(e -> AnnotationSupport.findAnnotation(e, type));
        if (onElement.isPresent()) {
            return onElement;
        }
        return context.getTestClass().flatMap(c -> AnnotationSupport.findAnnotation(c, type));
    }

    // ---------------------------------------------------------------- UnifiedPage injection

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == UnifiedPage.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        TestingTool tool = Settings.tool(context);
        Duration timeout = Settings.timeout(context);
        TestingToolAdapter adapter = adapterFor(tool);
        AdapterSession session = adapter.open(new AdapterContext() {
            @Override public ExtensionContext extensionContext() { return context; }
            @Override public Optional<String> configuration(String key) { return Settings.get(context, key); }
            @Override public Duration timeout() { return timeout; }
        });
        context.getStore(NAMESPACE).put(SESSION, session);
        return new UnifiedPageImpl(new TestRun(tool, session, timeout));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        AdapterSession session = context.getStore(NAMESPACE).remove(SESSION, AdapterSession.class);
        if (session != null) {
            session.close();
        }
    }

    private static TestingToolAdapter adapterFor(TestingTool tool) {
        for (TestingToolAdapter adapter : ServiceLoader.load(TestingToolAdapter.class)) {
            if (adapter.tool() == tool) {
                return adapter;
            }
        }
        throw new IllegalStateException("No adapter for vaadin.testing.tool=" + tool.name().toLowerCase()
                + ". Add the test dependency com.vaadin:unified-test-" + tool.name().toLowerCase()
                + (tool == TestingTool.SELENIUM ? " (not available yet)" : "") + ".");
    }
}
