package com.vaadin.testing.unified.internal;

import com.vaadin.testing.unified.spi.Filter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code @UnifiedComponent(selector = ...)}: a compound CSS selector that every Testing Tool applies, turned
 * into the query's filters. Supported: {@code .class}, {@code #id}, {@code [attribute="value"]} and
 * {@code [theme~="value"]}, chained without spaces, e.g. {@code .metric-card[data-kind="total"]}.
 */
final class WrapperSelector {
    private WrapperSelector() {}

    private static final String NAME = "[A-Za-z_][-A-Za-z0-9_]*";
    private static final Pattern PART = Pattern.compile("\\.(" + NAME + ")|#(" + NAME + ")"
            + "|\\[(" + NAME + ")(~?)=\"((?:[^\"\\\\]|\\\\.)*)\"\\]");

    static List<Filter> filters(String selector, String wrapperName) {
        List<Filter> filters = new ArrayList<>();
        Matcher m = PART.matcher(selector.strip());
        int at = 0;
        while (at < selector.strip().length()) {
            if (!m.find(at) || m.start() != at) {
                throw new IllegalArgumentException("@UnifiedComponent(selector = \"" + selector + "\") on "
                        + wrapperName + ": only .class, #id, [attribute=\"value\"] and [theme~=\"value\"],"
                        + " chained without spaces, work on every Testing Tool");
            }
            if (m.group(1) != null) {
                filters.add(new Filter(Filter.Kind.CLASS_NAME, null, m.group(1), true));
            } else if (m.group(2) != null) {
                filters.add(new Filter(Filter.Kind.ID, null, m.group(2), true));
            } else {
                String name = m.group(3);
                String value = m.group(5).replaceAll("\\\\(.)", "$1");
                if (!m.group(4).isEmpty()) {
                    if (!"theme".equals(name)) {
                        throw new IllegalArgumentException("@UnifiedComponent(selector = \"" + selector + "\") on "
                                + wrapperName + ": ~= works for theme only");
                    }
                    filters.add(new Filter(Filter.Kind.THEME, null, value, true));
                } else {
                    filters.add(new Filter(Filter.Kind.ATTRIBUTE, name, value, true));
                }
            }
            at = m.end();
        }
        return List.copyOf(filters);
    }
}
