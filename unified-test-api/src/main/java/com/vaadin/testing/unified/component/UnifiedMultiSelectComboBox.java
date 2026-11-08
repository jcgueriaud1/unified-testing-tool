package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.internal.selection.SelectionSupport;
import com.vaadin.testing.unified.query.LabelFilter;
import com.vaadin.testing.unified.query.PlaceholderFilter;
import com.vaadin.testing.unified.query.Query;
import java.util.List;

/** A {@link MultiSelectComboBox}. Items are identified by the label the user sees. */
@UnifiedComponent(MultiSelectComboBox.class)
public interface UnifiedMultiSelectComboBox extends UnifiedField,
        Findable<UnifiedMultiSelectComboBox.MultiSelectComboBoxQuery> {

    interface MultiSelectComboBoxQuery extends Query<UnifiedMultiSelectComboBox, MultiSelectComboBoxQuery>,
            LabelFilter<MultiSelectComboBoxQuery>, PlaceholderFilter<MultiSelectComboBoxQuery> {}

    // actions (Auto-Wait)
    /** Adds the items to the selection; already selected items stay selected. */
    default void selectItems(String... labels) {
        for (String label : labels) {
            SelectionSupport.select(this, label);
        }
    }

    /** Removes the items from the selection; items not selected are ignored. */
    default void deselectItems(String... labels) {
        for (String label : labels) {
            SelectionSupport.deselect(this, label);
        }
    }

    default void clear() { SelectionSupport.clear(this); }

    // getters (immediate)
    default List<String> getSelectedItems() { return SelectionSupport.access(this).selectedItems(); }

    /** Labels of the items to pick from; in a browser, the drop-down's first page (50 by default). */
    default List<String> getItems() { return SelectionSupport.access(this).items(); }

    default String getPlaceholder() { return element().getProperty("placeholder"); }
}
