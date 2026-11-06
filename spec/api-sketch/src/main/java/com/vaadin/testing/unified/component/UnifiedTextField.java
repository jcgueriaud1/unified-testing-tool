package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.PlaceholderFilter;
import com.vaadin.testing.unified.query.Query;

@UnifiedComponent(TextField.class)
public interface UnifiedTextField extends UnifiedWrapper, Findable<UnifiedTextField.TextFieldQuery> {

    interface TextFieldQuery extends Query<UnifiedTextField, TextFieldQuery>,
            LabelFilter<TextFieldQuery>, PlaceholderFilter<TextFieldQuery> {}

    // actions (Auto-Wait)
    default void setValue(String value) { element().setValueAsUser(value); }
    default void clear() { element().setValueAsUser(""); }

    // getters (immediate)
    default String getValue() { return element().getProperty("value"); }
    default String getLabel() { return element().getProperty("label"); }
    default String getPlaceholder() { return element().getProperty("placeholder"); }
    default String getHelperText() { return element().getProperty("helperText"); }
    default boolean isInvalid() { return Boolean.TRUE.equals(element().getPropertyBoolean("invalid")); }
    default String getErrorMessage() { return element().getProperty("errorMessage"); }
    default boolean isRequired() { return Boolean.TRUE.equals(element().getPropertyBoolean("required")); }
}
