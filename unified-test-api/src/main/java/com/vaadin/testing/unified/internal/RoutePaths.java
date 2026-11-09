package com.vaadin.testing.unified.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RoutePrefix;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Route class to URL path, resolved statically from the annotations (spec 03-navigation): {@code @Route}
 * value or Flow's naming convention, prefixes of the parent layouts ({@code @RoutePrefix}), absolute routes.
 * For browser Testing Tools that have no route navigation of their own.
 */
public final class RoutePaths {
    private RoutePaths() {}

    private static final String NAMING_CONVENTION = "___NAMING_CONVENTION___";

    /** The path without leading slash, e.g. {@code "admin/users"}; {@code ""} for the root. */
    public static String pathOf(Class<? extends Component> view) {
        Route route = view.getAnnotation(Route.class);
        if (route == null) {
            throw new IllegalArgumentException(view.getName() + " has no @Route");
        }
        List<String> parts = new ArrayList<>();
        if (!route.absolute()) {
            parts.addAll(prefixes(route.layout()));
        }
        parts.add(NAMING_CONVENTION.equals(route.value()) ? conventionName(view) : route.value());
        return join(parts);
    }

    public static String pathOf(Class<? extends Component> view, Object urlParameter) {
        String path = pathOf(view);
        return urlParameter == null ? path : join(List.of(path, String.valueOf(urlParameter)));
    }

    /** Fills {@code :name} segments of the route template; missing optional ones are dropped. */
    public static String pathOf(Class<? extends Component> view, RouteParameters parameters) {
        List<String> segments = new ArrayList<>();
        for (String segment : pathOf(view).split("/")) {
            if (!segment.startsWith(":")) {
                segments.add(segment);
                continue;
            }
            String name = segment.substring(1).replaceAll("[?*]?(\\(.*\\))?$", "").replaceAll("[?*]$", "");
            parameters.get(name).ifPresent(segments::add);
        }
        return join(segments);
    }

    private static List<String> prefixes(Class<?> layout) {
        List<String> prefixes = new ArrayList<>();
        for (Class<?> l = layout; l != null && l != com.vaadin.flow.component.UI.class; ) {
            RoutePrefix prefix = l.getAnnotation(RoutePrefix.class);
            if (prefix != null) {
                prefixes.add(0, prefix.value());
                if (prefix.absolute()) {
                    break;
                }
            }
            ParentLayout parent = l.getAnnotation(ParentLayout.class);
            l = parent == null ? null : parent.value();
        }
        return prefixes;
    }

    /** Flow's naming convention: the class name without a "View" suffix, lower case; "Main" is the root. */
    private static String conventionName(Class<?> view) {
        String name = view.getSimpleName();
        if (name.endsWith("View") && name.length() > 4) {
            name = name.substring(0, name.length() - 4);
        }
        return "Main".equals(name) ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static String join(List<String> parts) {
        return String.join("/", parts.stream().map(RoutePaths::strip).filter(p -> !p.isEmpty()).toList());
    }

    private static String strip(String path) {
        String p = path == null ? "" : path;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}
