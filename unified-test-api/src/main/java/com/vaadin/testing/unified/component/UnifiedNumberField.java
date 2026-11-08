package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.PlaceholderFilter;
import com.vaadin.testing.unified.query.Query;

/** A {@link NumberField}: the value is a {@link Double}, {@code null} when empty. */
@UnifiedComponent(NumberField.class)
public interface UnifiedNumberField extends UnifiedField, Findable<UnifiedNumberField.NumberFieldQuery> {

    interface NumberFieldQuery extends Query<UnifiedNumberField, NumberFieldQuery>,
            LabelFilter<NumberFieldQuery>, PlaceholderFilter<NumberFieldQuery> {}

    // actions (Auto-Wait)
    /** Types the value as a user would; {@code null} empties the field. */
    default void setValue(Double value) { element().setValueAsUser(value); }
    default void clear() { element().setValueAsUser(null); }

    // getters (immediate)
    default Double getValue() { return toDouble(element().getProperty("value")); }
    default String getPlaceholder() { return element().getProperty("placeholder"); }
    default Double getMin() { return toDouble(element().getProperty("min")); }
    default Double getMax() { return toDouble(element().getProperty("max")); }
    default Double getStep() { return toDouble(element().getProperty("step")); }

    private static Double toDouble(String value) {
        return value == null || value.isBlank() || "null".equals(value) ? null : Double.valueOf(value);
    }
}
