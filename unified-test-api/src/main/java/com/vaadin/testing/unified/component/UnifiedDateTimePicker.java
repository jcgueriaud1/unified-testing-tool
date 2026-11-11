package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.internal.Wrappers;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.spi.DateTimePickerAccess;
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

    /**
     * Types this text into the date half and the time half as it is, without parsing it, then commits it:
     * for testing how the app handles input it can't parse ({@code typeText("dsdds", "10:00")}) or an
     * incomplete one ({@code typeText("2026-11-08", "")}). An empty text leaves that half empty. Text the
     * field can't parse, or only one half filled, leaves the value empty and the field invalid, on every
     * Testing Tool. Browserless parses only ISO text ({@code 2026-11-08}, {@code 10:30}); a browser parses the
     * text in the field's locale format.
     */
    default void typeText(String date, String time) {
        Wrappers.lazyElementOf(this).act("type \"" + date + "\" / \"" + time + "\" in",
                e -> e.unwrap(DateTimePickerAccess.class).typeText(date, time));
    }

    // getters (immediate)
    /** The value property is ISO ({@code 2026-11-08T10:30}) in the browser and on the server. */
    default LocalDateTime getValue() {
        String value = element().getProperty("value");
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }
}
