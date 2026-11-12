package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.testing.unified.spi.SelectionAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
import org.vaadin.addons.dramafinder.element.MultiSelectComboBoxElement;
import org.vaadin.addons.dramafinder.element.RadioButtonGroupElement;

/** Selection on Playwright through Dramafinder's elements: labels are the ones the user sees. */
final class PlaywrightSelectionAccess implements SelectionAccess {

    enum Kind { COMBO_BOX, MULTI_SELECT_COMBO_BOX, RADIO_BUTTON_GROUP }

    private final PlaywrightSession session;
    private final Locator locator;
    private final Kind kind;

    PlaywrightSelectionAccess(PlaywrightSession session, Locator locator, Kind kind) {
        this.session = session;
        this.locator = locator;
        this.kind = kind;
    }

    @Override
    public List<String> selectedItems() {
        return switch (kind) {
            case COMBO_BOX -> optional(new ComboBoxElement(locator).getSelectedItemLabel());
            case MULTI_SELECT_COMBO_BOX -> new MultiSelectComboBoxElement(locator).getSelectedItems();
            case RADIO_BUTTON_GROUP -> optional(new RadioButtonGroupElement(locator).getSelectedLabel());
        };
    }

    private static List<String> optional(String label) {
        return label == null ? List.of() : List.of(label);
    }

    /** Every item, a combo box's lazily loaded pages included. */
    @Override
    public List<String> items() {
        return switch (kind) {
            case COMBO_BOX -> new ComboBoxElement(locator).getItemLabels();
            case MULTI_SELECT_COMBO_BOX -> new MultiSelectComboBoxElement(locator).getItemLabels();
            case RADIO_BUTTON_GROUP -> new RadioButtonGroupElement(locator).getItemLabels();
        };
    }

    @Override
    public void select(String label) {
        switch (kind) {
            case COMBO_BOX -> new ComboBoxElement(locator).selectItem(label);
            case MULTI_SELECT_COMBO_BOX -> {
                MultiSelectComboBoxElement multi = new MultiSelectComboBoxElement(locator);
                multi.selectItem(label);
                multi.close();
            }
            case RADIO_BUTTON_GROUP -> new RadioButtonGroupElement(locator).selectByLabel(label);
        }
        AutoWait.waitForVaadinIdle(session.page());
    }

    @Override
    public void deselect(String label) {
        MultiSelectComboBoxElement multi = new MultiSelectComboBoxElement(locator);
        multi.deselectItem(label);
        multi.close();
        AutoWait.waitForVaadinIdle(session.page());
    }

    /** What a user does: the clear button when shown, otherwise empty the input or deselect every item. */
    @Override
    public void clear() {
        switch (kind) {
            case COMBO_BOX -> new ComboBoxElement(locator).clear();
            case MULTI_SELECT_COMBO_BOX -> new MultiSelectComboBoxElement(locator).clear();
            case RADIO_BUTTON_GROUP -> throw new UnsupportedOperationException("A user can't clear a radio button group");
        }
    }
}
