package com.vaadin.testing.unified.internal;

import com.vaadin.testing.unified.spi.Filter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * What the browser adapters share to find elements in one round trip: filters that CSS can express go into
 * the selector; the others are checked by {@link #MATCH} in the page, over every candidate at once.
 */
public final class BrowserQueries {
    private BrowserQueries() {}

    private static final Pattern ATTRIBUTE_NAME = Pattern.compile("[A-Za-z_][-A-Za-z0-9_]*");

    /** The selector part for a filter CSS can express, or null. */
    public static String selectorPart(Filter f) {
        if (!f.exact()) {
            return null;
        }
        return switch (f.kind()) {
            case ID -> "[id=" + quote(f.value()) + "]";
            case TEST_ID -> "[data-testid=" + quote(f.value()) + "]";
            case CLASS_NAME -> f.value().isBlank() || f.value().matches(".*\\s.*") ? null
                    : "[class~=" + quote(f.value()) + "]";
            case THEME -> f.value().isBlank() || f.value().matches(".*\\s.*") ? null
                    : "[theme~=" + quote(f.value()) + "]";
            case ATTRIBUTE -> ATTRIBUTE_NAME.matcher(f.name()).matches() ? "[" + f.name() + "=" + quote(f.value()) + "]"
                    : null;
            default -> null;
        };
    }

    /** The selector with every filter CSS can express. */
    public static String selector(String tag, List<Filter> filters) {
        StringBuilder selector = new StringBuilder(tag);
        for (Filter f : filters) {
            String part = selectorPart(f);
            if (part != null) {
                selector.append(part);
            }
        }
        return selector.toString();
    }

    /** The filters the selector can't express, as {@link #MATCH} takes them. */
    public static List<Map<String, Object>> remaining(List<Filter> filters) {
        List<Map<String, Object>> rest = new ArrayList<>();
        for (Filter f : filters) {
            if (selectorPart(f) == null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("kind", f.kind().name());
                m.put("name", f.name() == null ? "" : f.name());
                m.put("value", f.value());
                m.put("exact", f.exact());
                rest.add(m);
            }
        }
        return rest;
    }

    /**
     * XPath for light-DOM children ({@code descendants}: every level) with this tag, {@code *} for any. XPath,
     * unlike CSS in Playwright, never enters shadow roots.
     */
    public static String lightDomXPath(String tag, boolean descendants) {
        String axis = descendants ? ".//*" : "./*";
        if ("*".equals(tag)) {
            return axis;
        }
        if (!tag.matches("[A-Za-z][-A-Za-z0-9_.]*")) {
            throw new IllegalArgumentException("Not a tag name: " + tag);
        }
        return axis + "[local-name()='" + tag.toLowerCase(java.util.Locale.ROOT) + "']";
    }

    /** Dash-separated CSS property name ({@code backgroundColor} becomes {@code background-color}). */
    public static String cssName(String name) {
        return name.startsWith("--") ? name : name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(java.util.Locale.ROOT);
    }

    /** Browser function {@code (element, name) => value}: the inline style's declared value, or null. */
    public static final String INLINE_STYLE = """
            (e, n) => {
              const v = e.style.getPropertyValue(n);
              if (v && v.trim() !== '') return v.trim();
              for (const part of (e.getAttribute('style') || '').split(';')) {
                const i = part.indexOf(':');
                if (i > 0 && part.substring(0, i).trim() === n) return part.substring(i + 1).trim();
              }
              return null;
            }""";

    /** Whitespace collapsed and trimmed, as {@link #TEXT} does in the browser. */
    public static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").strip();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * The element's text: its DOM text ({@code textContent}), whitespace collapsed. The same on every Testing
     * Tool: Browserless reads the same text from the server-side elements.
     */
    public static final String TEXT = "e => (e.textContent || '').replace(/\\s+/g, ' ').trim()";

    /**
     * What a screen reader announces, approximated as on every Testing Tool: aria-label, the accessibleName
     * property ({@code setAriaLabel} on a field), the aria-labelledby target's text, label, text.
     */
    public static final String ACCESSIBLE_NAME = """
            e => {
              const text = %s;
              const by = e.getAttribute('aria-labelledby');
              const target = by && document.getElementById(by);
              for (const v of [e.getAttribute('aria-label'), e.accessibleName, target ? text(target) : null, e.label]) {
                if (v != null && String(v).trim() !== '') return String(v);
              }
              return text(e);
            }""".formatted(TEXT);

    /**
     * Browser function {@code (elements, filters) => indices}: the indices of the elements matching every
     * filter of {@link #remaining}. Matching is as in {@link Filter#matches}.
     */
    public static final String MATCH = """
            (elements, filters) => {
              const text = %s;
              const accessibleName = %s;
              const norm = v => v == null ? null : String(v).replace(/\\s+/g, ' ').trim();
              const matches = (actual, f) => {
                const a = norm(actual);
                if (a == null) return false;
                return f.exact ? a === f.value : a.toLowerCase().includes(f.value.toLowerCase());
              };
              const check = (e, f) => {
                switch (f.kind) {
                  case 'TEXT': return matches(text(e), f);
                  case 'LABEL': return matches(e.label, f);
                  case 'PLACEHOLDER': return matches(e.placeholder, f);
                  case 'ACCESSIBLE_NAME': return matches(accessibleName(e), f);
                  case 'TEST_ID': return e.getAttribute('data-testid') === f.value;
                  case 'ID': return e.getAttribute('id') === f.value;
                  case 'CLASS_NAME': return e.classList.contains(f.value);
                  case 'THEME': return (e.getAttribute('theme') || '').split(/\\s+/).includes(f.value);
                  case 'ATTRIBUTE': return e.getAttribute(f.name) === f.value;
                  default: return false;
                }
              };
              const found = [];
              elements.forEach((e, i) => { if (filters.every(f => check(e, f))) found.push(i); });
              return found;
            }""".formatted(TEXT, ACCESSIBLE_NAME);
}
