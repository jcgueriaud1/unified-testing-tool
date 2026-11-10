package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Loaded only when browserless-test-spring is on the classpath ({@link SpringSupport#hasBrowserlessSpring}). */
final class BrowserlessSpringSupport {
    private BrowserlessSpringSupport() {}

    /**
     * The test's page on the Browserless app built from its Spring context. With Spring Security in the
     * context the app is secured and the page belongs to the test's user ({@code @WithMockUser},
     * {@code @WithUserDetails}), or to an anonymous user.
     *
     * @param viewPackages the configured view packages, or null for the application's packages
     */
    static BrowserlessSession open(ExtensionContext context, String[] viewPackages, Class<?> testClass) {
        ApplicationContext spring = SpringExtension.getApplicationContext(context);
        String[] packages = viewPackages != null ? viewPackages : SpringSupport.applicationPackages(spring, testClass);
        if (SecuritySupport.isSecured(spring)) {
            return SecuritySupport.open(spring, packages);
        }
        BrowserlessApplicationContext app = SpringBrowserlessApplicationContext.create(spring, packages);
        return new BrowserlessSession(app, app.newUser());
    }

}
