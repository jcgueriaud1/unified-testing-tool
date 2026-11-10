package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.PlaceholderFilter;
import com.vaadin.testing.unified.query.Query;
import java.math.BigDecimal;

/** A {@link BigDecimalField}: the value is a {@link BigDecimal}, {@code null} when empty. */
@UnifiedComponent(BigDecimalField.class)
public interface UnifiedBigDecimalField extends UnifiedField, Findable<UnifiedBigDecimalField.BigDecimalFieldQuery> {

    interface BigDecimalFieldQuery extends Query<UnifiedBigDecimalField, BigDecimalFieldQuery>,
            LabelFilter<BigDecimalFieldQuery>, PlaceholderFilter<BigDecimalFieldQuery> {}

    // actions (Auto-Wait)
    /** Types the value as a user would; {@code null} empties the field. */
    default void setValue(BigDecimal value) { element().setValueAsUser(value); }
    default void clear() { element().setValueAsUser(null); }

    // getters (immediate)
    /** Compare with {@code compareTo}: the scale is what the user typed ("12.50"). */
    default BigDecimal getValue() {
        String value = element().getProperty("value");
        return value == null || value.isBlank() || "null".equals(value) ? null : new BigDecimal(value.strip());
    }

    default String getPlaceholder() { return element().getProperty("placeholder"); }
}
