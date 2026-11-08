package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.Query;

@UnifiedComponent(Checkbox.class)
public interface UnifiedCheckbox extends UnifiedWrapper, Findable<UnifiedCheckbox.CheckboxQuery> {

    interface CheckboxQuery extends Query<UnifiedCheckbox, CheckboxQuery>, LabelFilter<CheckboxQuery> {}

    // actions (Auto-Wait); no-ops when already in that state
    default void check() { setChecked(true); }
    default void uncheck() { setChecked(false); }
    default void setChecked(boolean checked) { element().setValueAsUser(checked); }

    // getters (immediate)
    default boolean isChecked() { return Boolean.TRUE.equals(element().getPropertyBoolean("checked")); }
    default boolean isIndeterminate() { return Boolean.TRUE.equals(element().getPropertyBoolean("indeterminate")); }
    default String getLabel() { return element().getProperty("label"); }
    default String getHelperText() { return element().getProperty("helperText"); }
}
