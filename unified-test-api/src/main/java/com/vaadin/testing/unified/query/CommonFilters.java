package com.vaadin.testing.unified.query;

import com.vaadin.testing.unified.UnifiedElement;
import java.util.function.Predicate;

/**
 * Filters every type has. Exact match by default; {@code ...Containing} for partial matches.
 * <p>
 * Queries include hidden components, on every Testing Tool: a component hidden after it was shown stays in a
 * browser's page. Add {@link #visible()} where that matters, e.g. "the action is gone". A component that was
 * never visible is in no browser's page (Flow doesn't send it), but Browserless finds it; {@code visible()}
 * gives the same answer everywhere.
 */
public interface CommonFilters<SELF> {
    /** Visible components only: {@link com.vaadin.testing.unified.UnifiedElement#isVisible()}. */
    SELF visible();

    /** Recommended for every type: what a screen reader announces. */
    SELF withAccessibleName(String name);
    SELF withAccessibleNameContaining(String part);

    /** Fallbacks for components without a user-visible name. */
    SELF withTestId(String testId);
    SELF withId(String id);

    /** Supported, not recommended: implementation details. */
    SELF withClassName(String className);
    SELF withTheme(String theme);
    SELF withAttribute(String name, String value);

    /** Building blocks for custom filters. */
    SELF withProperty(String name, Object value);
    SELF where(Predicate<UnifiedElement> condition);
}
