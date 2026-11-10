package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.combobox.ComboBoxTester;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxTester;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioButtonGroupTester;
import com.vaadin.testing.unified.spi.SelectionAccess;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Selection on Browserless through the testers, which work with item labels. Labels come from the
 * component's item label generator.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class BrowserlessSelectionAccess implements SelectionAccess {

    private final BrowserlessSession session;
    private final Component component;

    BrowserlessSelectionAccess(BrowserlessSession session, Component component) {
        this.session = session;
        this.component = component;
    }

    private ComponentTester<?> tester() {
        return session.window().test(component);
    }

    private ItemLabelGenerator<Object> labels() {
        if (component instanceof ComboBoxBase<?, ?, ?> box) {
            return (ItemLabelGenerator<Object>) box.getItemLabelGenerator();
        }
        return (ItemLabelGenerator<Object>) ((RadioButtonGroup<?>) component).getItemLabelGenerator();
    }

    @Override
    public List<String> selectedItems() {
        Object value = ((AbstractField<?, ?>) component).getValue();
        if (value == null) {
            return List.of();
        }
        Collection<Object> items = value instanceof Collection<?> c ? (Collection<Object>) c : List.of(value);
        return items.stream().map(labels()::apply).toList();
    }

    @Override
    public List<String> items() {
        if (component instanceof RadioButtonGroup<?> group) {
            return group.getGenericDataView().getItems().map(i -> labels().apply(i)).toList();
        }
        return tester() instanceof ComboBoxTester t ? t.getSuggestions()
                : ((MultiSelectComboBoxTester) tester()).getSuggestions();
    }

    @Override
    public void select(String label) {
        session.act(() -> {
            switch (tester()) {
                case ComboBoxTester t -> t.selectItem(label);
                case MultiSelectComboBoxTester t -> {
                    // the tester's selectItem replaces the selection; a user's click adds to it
                    List<String> labels = new java.util.ArrayList<>(selectedItems());
                    labels.add(label);
                    t.selectItem(labels.toArray(String[]::new));
                }
                case RadioButtonGroupTester t -> t.selectItem(label);
                default -> throw new UnsupportedOperationException(component.getClass().getName());
            }
            session.window().roundTrip();
        });
    }

    /**
     * MultiSelectComboBoxTester has no deselect: removes the item on the server (the value change is not
     * marked as from the client). Gap ticket #61.
     */
    @Override
    public void deselect(String label) {
        MultiSelectComboBox<Object> multi = (MultiSelectComboBox<Object>) component;
        multi.getValue().stream().filter(i -> Objects.equals(labels().apply(i), label)).findFirst()
                .ifPresent(multi::deselect);
        session.window().roundTrip();
    }

    /** The clear button when it is shown, as a user would; otherwise empties the value on the server. */
    @Override
    public void clear() {
        session.act(() -> {
            if (component instanceof ComboBoxBase<?, ?, ?> box && box.isClearButtonVisible()) {
                if (tester() instanceof ComboBoxTester t) {
                    t.clickClearButton();
                } else {
                    ((MultiSelectComboBoxTester) tester()).clickClearButton();
                }
            } else if (component instanceof ComboBox<?> || component instanceof MultiSelectComboBox<?>) {
                ((AbstractField<?, ?>) component).clear();
            } else {
                throw new UnsupportedOperationException("A user can't clear a " + component.getClass().getSimpleName());
            }
            session.window().roundTrip();
        });
    }
}
