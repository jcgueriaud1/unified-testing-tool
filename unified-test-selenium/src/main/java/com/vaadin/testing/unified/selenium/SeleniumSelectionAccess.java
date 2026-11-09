package com.vaadin.testing.unified.selenium;

import com.vaadin.flow.component.combobox.testbench.ComboBoxElement;
import com.vaadin.flow.component.combobox.testbench.MultiSelectComboBoxElement;
import com.vaadin.flow.component.radiobutton.testbench.RadioButtonGroupElement;
import com.vaadin.testing.unified.spi.SelectionAccess;
import java.util.List;
import org.openqa.selenium.WebElement;

/**
 * Selection on Selenium: actions through TestBench's element classes, labels read from the web component
 * (the label Flow sends with each item).
 */
final class SeleniumSelectionAccess implements SelectionAccess {

    enum Kind { COMBO_BOX, MULTI_SELECT_COMBO_BOX, RADIO_BUTTON_GROUP }

    private final SeleniumSession session;
    private final WebElement element;
    private final Kind kind;

    SeleniumSelectionAccess(SeleniumSession session, WebElement element, Kind kind) {
        this.session = session;
        this.element = element;
        this.kind = kind;
    }

    @SuppressWarnings("unchecked")
    private List<String> strings(String script) {
        return (List<String>) session.js(script, element);
    }

    @Override
    public List<String> selectedItems() {
        return strings(switch (kind) {
            case COMBO_BOX -> "const c = arguments[0]; return c.selectedItem ? [String(c.selectedItem[c.itemLabelPath])] : [];";
            case MULTI_SELECT_COMBO_BOX -> "const c = arguments[0]; return (c.selectedItems || []).map(i => String(i[c.itemLabelPath]));";
            case RADIO_BUTTON_GROUP -> "return [...arguments[0].querySelectorAll('vaadin-radio-button')].filter(r => r.checked)"
                    + ".map(r => (r.label || r.textContent).trim());";
        });
    }

    /** Combo boxes: opens the drop-down, waits for its items, reads the loaded ones, closes it. */
    @Override
    public List<String> items() {
        if (kind == Kind.RADIO_BUTTON_GROUP) {
            return strings("return [...arguments[0].querySelectorAll('vaadin-radio-button')]"
                    + ".map(r => (r.label || r.textContent).trim());");
        }
        boolean wasOpen = Boolean.TRUE.equals(session.js("return !!arguments[0].opened;", element));
        if (!wasOpen) {
            session.js("arguments[0].opened = true;", element);
        }
        try {
            waitUntil("const c = arguments[0]; return c.opened && !c.loading;");
            return strings("const c = arguments[0]; return (c.filteredItems || [])"
                    + ".filter(i => i && i[c.itemLabelPath] !== undefined).map(i => String(i[c.itemLabelPath]));");
        } finally {
            if (!wasOpen) {
                session.js("arguments[0].opened = false;", element);
            }
        }
    }

    private void waitUntil(String script) {
        long deadline = System.nanoTime() + session.timeout().toNanos();
        while (!Boolean.TRUE.equals(session.js(script, element))) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("Timed out waiting for the drop-down's items");
            }
            session.waitForVaadin();
        }
    }

    @Override
    public void select(String label) {
        switch (kind) {
            case COMBO_BOX -> session.testBench(element).wrap(ComboBoxElement.class).selectByText(label);
            case MULTI_SELECT_COMBO_BOX -> {
                MultiSelectComboBoxElement multi = session.testBench(element).wrap(MultiSelectComboBoxElement.class);
                multi.selectByText(label);
                multi.closePopup();
            }
            case RADIO_BUTTON_GROUP -> session.testBench(element).wrap(RadioButtonGroupElement.class).selectByText(label);
        }
        session.waitForVaadin();
    }

    @Override
    public void deselect(String label) {
        MultiSelectComboBoxElement multi = session.testBench(element).wrap(MultiSelectComboBoxElement.class);
        multi.deselectByText(label);
        multi.closePopup();
        session.waitForVaadin();
    }

    /** The clear button when shown, as a user would; otherwise TestBench's clear. */
    @Override
    public void clear() {
        if (kind == Kind.RADIO_BUTTON_GROUP) {
            throw new UnsupportedOperationException("A user can't clear a radio button group");
        }
        boolean clicked = Boolean.TRUE.equals(session.js("""
                const c = arguments[0];
                const button = c.clearButtonVisible && c.shadowRoot && c.shadowRoot.querySelector("[part~='clear-button']");
                if (button) { button.click(); return true; }
                return false;""", element));
        if (!clicked) {
            if (kind == Kind.COMBO_BOX) {
                session.testBench(element).wrap(ComboBoxElement.class).clear();
            } else {
                session.testBench(element).wrap(MultiSelectComboBoxElement.class).deselectAll();
            }
        }
        session.waitForVaadin();
    }
}
