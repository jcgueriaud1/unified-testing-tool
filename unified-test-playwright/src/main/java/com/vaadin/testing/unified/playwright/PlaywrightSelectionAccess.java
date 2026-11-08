package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.testing.unified.spi.SelectionAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
import org.vaadin.addons.dramafinder.element.MultiSelectComboBoxElement;
import org.vaadin.addons.dramafinder.element.RadioButtonGroupElement;

/**
 * Selection on Playwright through Dramafinder's elements for actions; labels are read from the web
 * component (the label Flow sends with each item).
 */
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

    @SuppressWarnings("unchecked")
    private List<String> strings(String script) {
        return (List<String>) locator.evaluate(script);
    }

    @Override
    public List<String> selectedItems() {
        return strings(switch (kind) {
            case COMBO_BOX -> "c => c.selectedItem ? [String(c.selectedItem[c.itemLabelPath])] : []";
            case MULTI_SELECT_COMBO_BOX -> "c => (c.selectedItems || []).map(i => String(i[c.itemLabelPath]))";
            case RADIO_BUTTON_GROUP -> "g => [...g.querySelectorAll('vaadin-radio-button')].filter(r => r.checked)"
                    + ".map(r => (r.label || r.textContent).trim())";
        });
    }

    /** Combo boxes: opens the drop-down, waits for its items, reads the loaded ones, closes it. */
    @Override
    public List<String> items() {
        if (kind == Kind.RADIO_BUTTON_GROUP) {
            return strings("g => [...g.querySelectorAll('vaadin-radio-button')]"
                    + ".map(r => (r.label || r.textContent).trim())");
        }
        boolean wasOpen = Boolean.TRUE.equals(locator.evaluate("c => c.opened"));
        if (!wasOpen) {
            locator.evaluate("c => c.opened = true");
        }
        try {
            session.page().waitForFunction("c => c.opened && !c.loading", locator.elementHandle());
            return strings("c => (c.filteredItems || [])"
                    + ".filter(i => i && i[c.itemLabelPath] !== undefined).map(i => String(i[c.itemLabelPath]))");
        } finally {
            if (!wasOpen) {
                locator.evaluate("c => c.opened = false");
            }
        }
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

    /** What a user does: the clear button when shown, otherwise empty the input and leave the field. */
    @Override
    public void clear() {
        if (kind == Kind.RADIO_BUTTON_GROUP) {
            throw new UnsupportedOperationException("A user can't clear a radio button group");
        }
        if (Boolean.TRUE.equals(locator.evaluate("c => c.clearButtonVisible"))) {
            locator.locator("[part~='clear-button']").click();
        } else if (kind == Kind.COMBO_BOX) {
            Locator input = locator.locator("input");
            input.fill("");
            input.blur();
        } else {
            for (String label : selectedItems()) {
                deselect(label);
            }
        }
        AutoWait.waitForVaadinIdle(session.page());
    }
}
