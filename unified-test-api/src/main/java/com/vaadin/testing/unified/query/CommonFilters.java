package com.vaadin.testing.unified.query;

import com.vaadin.testing.unified.UnifiedElement;
import java.util.function.Predicate;

/** Filters every type has. Exact match by default; {@code ...Containing} for partial matches. */
public interface CommonFilters<SELF> {
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
