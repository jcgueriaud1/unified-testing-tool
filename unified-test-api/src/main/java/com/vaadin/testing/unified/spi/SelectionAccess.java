package com.vaadin.testing.unified.spi;

import java.util.List;

/**
 * Adapter access to a selection field (ComboBox, MultiSelectComboBox, RadioButtonGroup), reached by the core
 * with {@code element.unwrap(SelectionAccess.class)}. Items are identified by the label the user sees (the
 * item label generator's result).
 */
public interface SelectionAccess {

    /** Labels of the selected items: none or one for single selection, in display order for multi. */
    List<String> selectedItems();

    /** Labels of the items the user can pick from. */
    List<String> items();

    /** Selects the item as a user would; for multi selection, adds it. */
    void select(String label);

    /** Multi selection only: removes the item from the selection. */
    void deselect(String label);

    /** Empties the selection as a user would. */
    void clear();
}
