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

    /** The Browserless app built from the test's Spring context, or null if the test has none. */
    static BrowserlessApplicationContext applicationContext(ExtensionContext context, String[] viewPackages) {
        if (!hasSpringExtension(context)) {
            return null;
        }
        ApplicationContext spring = SpringExtension.getApplicationContext(context);
        return SpringBrowserlessApplicationContext.create(spring, viewPackages);
    }

    private static boolean hasSpringExtension(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        return org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(testClass,
                org.springframework.test.context.BootstrapWith.class)
                || org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(testClass,
                        org.springframework.test.context.ContextConfiguration.class);
    }
}
