package com.vaadin.testing.unified.spring;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.core.Ordered;

/**
 * Test-only endpoint {@value #PATH}{@code ?ui=<id>}: the classes of the views the caller's UI shows, one per
 * line (each view's class and superclasses, the route target first, then its layouts). Browser Testing Tools
 * use it for {@code isAt(view)} when the view has no route of its own (an error view). It answers only about
 * the caller's own Vaadin session.
 */
final class CurrentViewFilter implements Filter, Ordered {

    static final String PATH = com.vaadin.testing.unified.internal.CurrentView.PATH;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String path = http.getRequestURI().substring(http.getContextPath().length());
        if (!PATH.equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletResponse out = (HttpServletResponse) response;
        int uiId;
        try {
            uiId = Integer.parseInt(http.getParameter("ui"));
        } catch (NumberFormatException e) {
            out.sendError(HttpServletResponse.SC_BAD_REQUEST, "ui=<UI id> expected");
            return;
        }
        List<String> classes = classesOf(http.getSession(false), uiId);
        out.setContentType("text/plain");
        out.getWriter().write(String.join("\n", classes));
    }

    private static List<String> classesOf(HttpSession httpSession, int uiId) {
        if (httpSession == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String attribute : Collections.list(httpSession.getAttributeNames())) {
            if (!(httpSession.getAttribute(attribute) instanceof VaadinSession session)) {
                continue;
            }
            session.lock();
            try {
                UI ui = session.getUIById(uiId);
                if (ui == null) {
                    continue;
                }
                ui.getInternals().getActiveRouterTargetsChain().forEach(target -> {
                    for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                        names.add(c.getName());
                    }
                });
            } finally {
                session.unlock();
            }
        }
        return names;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
