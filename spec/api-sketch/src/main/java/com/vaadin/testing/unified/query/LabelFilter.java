package com.vaadin.testing.unified.query;

/** Components with a visible label: label property, a referring label element, or the FormLayout item label. */
public interface LabelFilter<SELF> {
    SELF withLabel(String label);
    SELF withLabelContaining(String part);
}
