package com.vaadin.testing.unified.spi;

/**
 * One filter of a query, as the adapter receives it. {@code exact} is false for the {@code ...Containing}
 * variants, which match case-insensitively (as in Dramafinder). {@code name} is used by {@link Kind#ATTRIBUTE}.
 */
public record Filter(Kind kind, String name, String value, boolean exact) {

    public enum Kind { LABEL, ACCESSIBLE_NAME, TEXT, PLACEHOLDER, TEST_ID, ID, CLASS_NAME, THEME, ATTRIBUTE }

    public boolean matches(String actual) {
        if (actual == null) {
            return false;
        }
        String normalized = actual.strip().replaceAll("\\s+", " ");
        return exact ? normalized.equals(value)
                : normalized.toLowerCase(java.util.Locale.ROOT).contains(value.toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public String toString() {
        String method = switch (kind) {
            case LABEL -> "withLabel";
            case ACCESSIBLE_NAME -> "withAccessibleName";
            case TEXT -> "withText";
            case PLACEHOLDER -> "withPlaceholder";
            case TEST_ID -> "withTestId";
            case ID -> "withId";
            case CLASS_NAME -> "withClassName";
            case THEME -> "withTheme";
            case ATTRIBUTE -> "withAttribute";
        };
        String arg = kind == Kind.ATTRIBUTE ? "\"" + name + "\", \"" + value + "\"" : "\"" + value + "\"";
        return method + (exact ? "" : "Containing") + "(" + arg + ")";
    }
}
