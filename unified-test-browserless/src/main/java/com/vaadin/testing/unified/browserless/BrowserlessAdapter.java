package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.spi.AdapterContext;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.TestingToolAdapter;
import java.util.Arrays;
import java.util.Optional;

/**
 * Browserless Test adapter. The application runs in the test JVM: from the test's Spring context when
 * there is one, otherwise from the view packages.
 * <p>
 * Setting {@code vaadin.testing.browserless.view-packages}: comma-separated packages to scan for routes.
 * Default: in a Spring test, the application's packages (those of its {@code @SpringBootApplication}) and
 * the test class's package; otherwise the test class's package.
 */
public final class BrowserlessAdapter implements TestingToolAdapter {

    static final String VIEW_PACKAGES = "vaadin.testing.browserless.view-packages";

    @Override
    public TestingTool tool() {
        return TestingTool.BROWSERLESS;
    }

    @Override
    public AdapterSession open(AdapterContext context) {
        Class<?> testClass = context.extensionContext().getRequiredTestClass();
        Optional<String[]> configured = context.configuration(VIEW_PACKAGES)
                .map(v -> Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new));

        if (SpringSupport.hasSpringTest()) {
            if (SpringSupport.isSpringTest(context.extensionContext())) {
                if (!SpringSupport.hasBrowserlessSpring()) {
                    throw new IllegalStateException("This is a Spring test, but com.vaadin:browserless-test-spring is"
                            + " not on the test classpath: without it Browserless builds the views without Spring and"
                            + " nothing is injected into them. Add the test dependency"
                            + " com.vaadin:browserless-test-spring (its version is in unified-test-bom).");
                }
                return BrowserlessSpringSupport.open(context.extensionContext(), configured.orElse(null), testClass);
            }
        }
        String[] viewPackages = configured.orElse(new String[] { testClass.getPackageName() });
        BrowserlessApplicationContext app = BrowserlessApplicationContext.create(viewPackages);
        return new BrowserlessSession(app, app.newUser());
    }
}
