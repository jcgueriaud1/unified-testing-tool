package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.PlaceholderFilter;
import com.vaadin.testing.unified.query.Query;

/** An {@link IntegerField}: the value is an {@link Integer}, {@code null} when empty. */
@UnifiedComponent(IntegerField.class)
public interface UnifiedIntegerField extends UnifiedField, Findable<UnifiedIntegerField.IntegerFieldQuery> {

    interface IntegerFieldQuery extends Query<UnifiedIntegerField, IntegerFieldQuery>,
            LabelFilter<IntegerFieldQuery>, PlaceholderFilter<IntegerFieldQuery> {}

    // actions (Auto-Wait)
    /** Types the value as a user would; {@code null} empties the field. */
    default void setValue(Integer value) { element().setValueAsUser(value); }
    default void clear() { element().setValueAsUser(null); }

    // getters (immediate)
    default Integer getValue() {
        String value = element().getProperty("value");
        return value == null || value.isBlank() || "null".equals(value) ? null
                : (int) Double.parseDouble(value);
    }

    default String getPlaceholder() { return element().getProperty("placeholder"); }
}
