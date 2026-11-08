package com.vaadin.testing.unified.playwright;

import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Spring-only code: base URL from the random port of {@code @UnifiedSpringBootTest}. */
final class SpringSupport {
    private SpringSupport() {}

    static boolean isAvailable() {
        try {
            Class.forName("org.springframework.test.context.junit.jupiter.SpringExtension");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    static Optional<String> baseUrl(ExtensionContext context) {
        if (!AnnotatedElementUtils.hasAnnotation(context.getRequiredTestClass(), BootstrapWith.class)) {
            return Optional.empty();
        }
        Environment env = SpringExtension.getApplicationContext(context).getEnvironment();
        String port = env.getProperty("local.server.port");
        if (port == null) {
            return Optional.empty();
        }
        String contextPath = env.getProperty("server.servlet.context-path", "");
        return Optional.of("http://localhost:" + port + contextPath);
    }
}
