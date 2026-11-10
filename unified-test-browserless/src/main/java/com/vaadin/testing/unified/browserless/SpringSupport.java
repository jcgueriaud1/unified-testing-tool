package com.vaadin.testing.unified.browserless;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;

/**
 * Spring-only code. {@link #hasSpringTest()} guards every other method: they run only when Spring's test
 * support is on the classpath. {@link BrowserlessSpringSupport} needs browserless-test-spring too.
 */
final class SpringSupport {
    private SpringSupport() {}

    static boolean hasSpringTest() {
        return present("org.springframework.test.context.junit.jupiter.SpringExtension");
    }

    static boolean hasBrowserlessSpring() {
        return present("com.vaadin.browserless.SpringBrowserlessApplicationContext");
    }

    private static boolean present(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * Where the application's routes are: the packages Spring Boot scans (those of its
     * {@code @SpringBootApplication}), plus the test's own package for test-only views.
     */
    static String[] applicationPackages(ApplicationContext spring, Class<?> testClass) {
        Set<String> packages = new LinkedHashSet<>(autoConfigurationPackages(spring));
        packages.add(testClass.getPackageName());
        return packages.toArray(String[]::new);
    }

    @SuppressWarnings("unchecked")
    private static List<String> autoConfigurationPackages(ApplicationContext spring) {
        try {
            Class<?> type = Class.forName("org.springframework.boot.autoconfigure.AutoConfigurationPackages");
            Object beanFactory = spring.getAutowireCapableBeanFactory();
            Class<?> factoryType = org.springframework.beans.factory.BeanFactory.class;
            if (!(Boolean) type.getMethod("has", factoryType).invoke(null, beanFactory)) {
                return List.of();
            }
            return (List<String>) type.getMethod("get", factoryType).invoke(null, beanFactory);
        } catch (ReflectiveOperationException | LinkageError e) {
            return List.of(); // not a Spring Boot app: only the test's package
        }
    }

    /** The test class or, for a {@code @Nested} class, an enclosing one is a Spring test. */
    static boolean isSpringTest(ExtensionContext context) {
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
