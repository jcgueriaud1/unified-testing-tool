package com.vaadin.testing.unified.playwright;

import com.vaadin.testing.unified.spring.TestLogin;
import java.util.Optional;

/**
 * Logging a browser in as a test user through the test-only endpoint of {@code @UnifiedSpringBootTest}
 * ({@link TestLogin}). Loaded only when unified-test-spring and Spring Security are on the classpath.
 */
final class LoginSupport {
    private LoginSupport() {}

    static boolean isAvailable() {
        try {
            Class.forName("com.vaadin.testing.unified.spring.TestLogin");
            return TestLogin.isAvailable();
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** The login URL for the test's user ({@code @WithMockUser}, {@code @WithUserDetails}), if any. */
    static Optional<String> testUserUrl(String baseUrl) {
        return isAvailable() ? TestLogin.tokenForTestUser().map(token -> url(baseUrl, token)) : Optional.empty();
    }

    static String userUrl(String baseUrl, String username, String... roles) {
        if (!isAvailable()) {
            throw new IllegalStateException("openAsUser needs @UnifiedSpringBootTest (unified-test-spring) and Spring"
                    + " Security on the test classpath");
        }
        return url(baseUrl, TestLogin.tokenFor(username, roles));
    }

    private static String url(String baseUrl, String token) {
        return baseUrl + TestLogin.PATH + "?token=" + token;
    }
}
