package com.vaadin.testing.unified.spring;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.SecurityContext;

/**
 * Serves {@link TestLogin#PATH}: puts the token's user into the HTTP session, where Spring Security's
 * session repository reads it on the next request. Runs before the security filter chain. Unknown or used
 * tokens get 403.
 */
final class TestLoginFilter implements Filter, Ordered {

    /** {@code HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY}. */
    static final String SESSION_KEY = "SPRING_SECURITY_CONTEXT";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String path = http.getRequestURI().substring(http.getContextPath().length());
        if (!TestLogin.PATH.equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletResponse out = (HttpServletResponse) response;
        SecurityContext user = TestLogin.take(http.getParameter("token"));
        if (user == null) {
            out.sendError(HttpServletResponse.SC_FORBIDDEN, "Unknown or used test login token");
            return;
        }
        if (http.getSession(false) != null) {
            http.changeSessionId(); // as a real login does
        }
        http.getSession(true).setAttribute(SESSION_KEY, user);
        out.setContentType("text/plain");
        out.getWriter().write("Logged in as " + user.getAuthentication().getName());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
