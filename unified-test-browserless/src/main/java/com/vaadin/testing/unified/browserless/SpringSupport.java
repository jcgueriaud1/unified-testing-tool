package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Spring-only code, loaded only when Spring test support and browserless-test-spring are on the classpath. */
final class SpringSupport {
    private SpringSupport() {}

    static boolean isAvailable() {
        try {
            Class.forName("org.springframework.test.context.junit.jupiter.SpringExtension");
            Class.forName("com.vaadin.browserless.SpringBrowserlessApplicationContext");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * The test's page on the Browserless app built from its Spring context, or null if the test has none.
     * With Spring Security in the context the app is secured and the page belongs to the test's user
     * ({@code @WithMockUser}, {@code @WithUserDetails}), or to an anonymous user.
     */
    static BrowserlessSession open(ExtensionContext context, String[] viewPackages) {
        if (!hasSpringExtension(context)) {
            return null;
        }
        ApplicationContext spring = SpringExtension.getApplicationContext(context);
        if (SecuritySupport.isSecured(spring)) {
            return SecuritySupport.open(spring, viewPackages);
        }
        BrowserlessApplicationContext app = SpringBrowserlessApplicationContext.create(spring, viewPackages);
        return new BrowserlessSession(app, app.newUser());
    }

    /** The test class or, for a {@code @Nested} class, an enclosing one is a Spring test. */
    private static boolean hasSpringExtension(ExtensionContext context) {
        for (Class<?> c = context.getRequiredTestClass(); c != null; c = c.getEnclosingClass()) {
            if (org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(c,
                    org.springframework.test.context.BootstrapWith.class)
                    || org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(c,
                            org.springframework.test.context.ContextConfiguration.class)) {
                return true;
            }
        }
        return false;
    }
}
