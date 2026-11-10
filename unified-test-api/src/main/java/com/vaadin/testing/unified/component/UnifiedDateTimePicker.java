package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.Query;
import java.time.LocalDateTime;

/** A {@link DateTimePicker}: the value is a {@link LocalDateTime}, {@code null} when empty. */
@UnifiedComponent(DateTimePicker.class)
public interface UnifiedDateTimePicker extends UnifiedField, Findable<UnifiedDateTimePicker.DateTimePickerQuery> {

    interface DateTimePickerQuery extends Query<UnifiedDateTimePicker, DateTimePickerQuery>,
            LabelFilter<DateTimePickerQuery> {}

    // actions (Auto-Wait)
    /** Enters the date and time as a user would; {@code null} empties the field. */
    default void setValue(LocalDateTime value) { element().setValueAsUser(value); }
    default void clear() { element().setValueAsUser(null); }

    // getters (immediate)
    /** The value property is ISO ({@code 2026-11-08T10:30}) in the browser and on the server. */
    default LocalDateTime getValue() {
        String value = element().getProperty("value");
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }
}
