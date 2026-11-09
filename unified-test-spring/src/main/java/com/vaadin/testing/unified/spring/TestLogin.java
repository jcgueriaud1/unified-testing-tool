package com.vaadin.testing.unified.spring;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;

/**
 * Logs a browser in as a test user (spec 11-users): the adapter registers the user here and gets a one-time
 * token, the browser opens {@link #PATH}{@code ?token=…}, and {@link TestLoginFilter} puts the user into the
 * HTTP session. Works because a {@code @UnifiedSpringBootTest} app runs in the test's JVM. Only in test
 * contexts: this module is a test dependency and never ships with the application.
 */
public final class TestLogin {
    private TestLogin() {}

    /** The endpoint, relative to the application's context path. */
    public static final String PATH = "/__unified-test/login";

    private static final Map<String, SecurityContext> PENDING = new ConcurrentHashMap<>();

    /** Spring Security is on the classpath: tests can log in. */
    public static boolean isAvailable() {
        try {
            Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * A token for the test's user ({@code @WithMockUser}, {@code @WithUserDetails}: Spring's test listener has
     * put it into the security context), or empty when the test runs anonymous.
     */
    public static Optional<String> tokenForTestUser() {
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        if (user == null || !user.isAuthenticated() || user instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(register(user));
    }

    /** The name of the test's user, or null when anonymous. */
    public static String testUserName() {
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        return user == null || user instanceof AnonymousAuthenticationToken ? null : user.getName();
    }

    /** A token for another user, with these roles ({@code ROLE_} prefix added), as {@code @WithMockUser} does. */
    public static String tokenFor(String username, String... roles) {
        var authorities = Arrays.stream(roles).map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        User principal = new User(username, "", authorities);
        return register(UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
    }

    private static String register(Authentication user) {
        String token = UUID.randomUUID().toString();
        PENDING.put(token, new SecurityContextImpl(user));
        return token;
    }

    /** The registered user of a token, once. */
    static SecurityContext take(String token) {
        return token == null ? null : PENDING.remove(token);
    }
}
