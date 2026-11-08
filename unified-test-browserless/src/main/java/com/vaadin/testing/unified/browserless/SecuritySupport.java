package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Spring Security code, loaded only when Spring Security is on the classpath. */
final class SecuritySupport {
    private SecuritySupport() {}

    /** Spring Security on the classpath and configured in the application. */
    static boolean isSecured(ApplicationContext spring) {
        try {
            Class<?> chain = Class.forName("org.springframework.security.web.SecurityFilterChain");
            return spring.getBeanNamesForType(chain).length > 0;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * The test's user: Spring's test listener has put {@code @WithMockUser} / {@code @WithUserDetails}
     * into the security context before the page is created.
     */
    static BrowserlessSession open(ApplicationContext spring, String[] viewPackages) {
        SecuredBrowserlessApplicationContext<Authentication> app =
                SpringBrowserlessApplicationContext.createSecured(spring, viewPackages);
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = user != null && user.isAuthenticated() && !(user instanceof AnonymousAuthenticationToken);
        return new BrowserlessSession(app, loggedIn ? app.newUser(user) : app.newUser());
    }
}
