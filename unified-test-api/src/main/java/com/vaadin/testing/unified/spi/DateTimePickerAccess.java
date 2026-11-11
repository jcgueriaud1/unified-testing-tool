package com.vaadin.testing.unified.spi;

/**
 * Adapter access to a DateTimePicker, reached by the core with
 * {@code element.unwrap(DateTimePickerAccess.class)}.
 */
public interface DateTimePickerAccess {

    /**
     * Types this text into each half as it is, without parsing it, and commits it as a user leaving the
     * field does; an empty text leaves that half empty.
     */
    void typeText(String date, String time);
}
