package com.vaadin.testing.unified.component;

import com.vaadin.testing.unified.UnifiedWrapper;

/** Getters shared by input fields: label, helper text and validation state. Immediate, no retry. */
public interface UnifiedField extends UnifiedWrapper {
    default String getLabel() { return element().getProperty("label"); }
    default String getHelperText() { return element().getProperty("helperText"); }
    default boolean isInvalid() { return Boolean.TRUE.equals(element().getPropertyBoolean("invalid")); }
    default String getErrorMessage() { return element().getProperty("errorMessage"); }
    default boolean isRequired() { return Boolean.TRUE.equals(element().getPropertyBoolean("required")); }
}
