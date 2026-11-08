package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.spi.AdapterContext;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.TestingToolAdapter;
import java.util.Arrays;

/**
 * Browserless Test adapter. The application runs in the test JVM: from the test's Spring context when
 * there is one, otherwise from the view packages.
 * <p>
 * Setting {@code vaadin.testing.browserless.view-packages}: comma-separated packages to scan for routes;
 * default: the test class's package.
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
        String[] viewPackages = context.configuration(VIEW_PACKAGES)
                .map(v -> Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new))
                .orElse(new String[] { testClass.getPackageName() });

        BrowserlessApplicationContext app = SpringSupport.isAvailable()
                ? SpringSupport.applicationContext(context.extensionContext(), viewPackages)
                : null;
        if (app == null) {
            app = BrowserlessApplicationContext.create(viewPackages);
        }
        BrowserlessUIContext window = app.newUser().newWindow();
        return new BrowserlessSession(app, window);
    }
}
