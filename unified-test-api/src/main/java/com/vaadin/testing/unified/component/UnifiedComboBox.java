package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.internal.selection.SelectionSupport;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.PlaceholderFilter;
import com.vaadin.testing.unified.query.Query;
import java.util.List;

/** A {@link ComboBox}. Items are identified by the label the user sees. */
@UnifiedComponent(ComboBox.class)
public interface UnifiedComboBox extends UnifiedField, Findable<UnifiedComboBox.ComboBoxQuery> {

    interface ComboBoxQuery extends Query<UnifiedComboBox, ComboBoxQuery>,
            LabelFilter<ComboBoxQuery>, PlaceholderFilter<ComboBoxQuery> {}

    // actions (Auto-Wait); fail when the item doesn't exist or isn't selected afterwards
    default void selectItem(String label) { SelectionSupport.select(this, label); }
    default void clear() { SelectionSupport.clear(this); }

    // getters (immediate)
    /** Label of the selected item, or null. */
    default String getSelectedItem() { return SelectionSupport.selected(this); }

    /**
     * Labels of the items to pick from. In a browser: the items the drop-down loads at once (its first
     * page, 50 by default).
     */
    default List<String> getItems() { return SelectionSupport.access(this).items(); }

    default String getPlaceholder() { return element().getProperty("placeholder"); }
}
