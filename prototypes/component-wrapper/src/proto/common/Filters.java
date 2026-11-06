// PROTOTYPE: per-type filter mixins (decided in #7, ADR 0001). A query or spec type only
// implements the mixins that make sense for its component, so wrong filters don't compile.
// Exact match by default; ...Containing for partial matches.
package proto.common;

public final class Filters {
    private Filters() {}

    /** Every type: fallbacks and discouraged-but-supported filters. */
    public interface CommonFilters<SELF> {
        SELF withTestId(String testId);      // fallback
        SELF withId(String id);              // fallback
        SELF withClassName(String name);     // supported, not recommended
        SELF withTheme(String theme);        // supported, not recommended
        SELF withAttribute(String name, String value); // supported, not recommended
        SELF withAccessibleName(String name);          // every type: what a screen reader announces
        SELF withAccessibleNameContaining(String part);
    }

    public interface LabelFilter<SELF> {
        SELF withLabel(String label);        // label property, <label for>, FormLayout item label
        SELF withLabelContaining(String part);
    }

    public interface TextFilter<SELF> {
        SELF withText(String text);          // visible text content
        SELF withTextContaining(String part);
    }

    public interface PlaceholderFilter<SELF> {
        SELF withPlaceholder(String placeholder);
    }
}
