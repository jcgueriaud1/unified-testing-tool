package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.internal.selection.SelectionSupport;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.Query;
import java.util.List;

/** A {@link RadioButtonGroup}. Items are identified by the radio button's label. */
@UnifiedComponent(RadioButtonGroup.class)
public interface UnifiedRadioButtonGroup extends UnifiedField,
        Findable<UnifiedRadioButtonGroup.RadioButtonGroupQuery> {

    interface RadioButtonGroupQuery extends Query<UnifiedRadioButtonGroup, RadioButtonGroupQuery>,
            LabelFilter<RadioButtonGroupQuery> {}

    // actions (Auto-Wait). A user can't deselect a radio button, so there is no clear().
    default void selectItem(String label) { SelectionSupport.select(this, label); }

    // getters (immediate)
    /** Label of the selected item, or null. */
    default String getSelectedItem() { return SelectionSupport.selected(this); }

    default List<String> getItems() { return SelectionSupport.access(this).items(); }
}
