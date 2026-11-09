package com.vaadin.testing.unified.selenium;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.testbench.DatePickerElement;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.testbench.NumberFieldElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testing.unified.spi.GridAccess;
import com.vaadin.testing.unified.spi.SelectionAccess;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.WebElement;

/**
 * The Selenium adapter's Core Set registrations: which tag stands for a component on the page, and how it
 * is read and acted on when the generic Unified Element behaviour is not enough. Actions go through
 * TestBench's own element classes where TestBench has one. Adding a Core Set component means one entry here.
 */
final class SeleniumComponents {
    private SeleniumComponents() {}

    /** How the Selenium adapter handles one component type. Every method has a generic default. */
    interface Support {
        /** CSS selector for the component on the page; by default its web component tag. */
        default String selector(String componentTag) {
            return componentTag;
        }

        default Optional<String> text(WebElement element, SeleniumSession session) {
            return Optional.empty();
        }

        default Optional<Boolean> visible(WebElement element, SeleniumSession session) {
            return Optional.empty();
        }

        default Optional<Boolean> booleanProperty(WebElement element, String name, SeleniumSession session) {
            return Optional.empty();
        }

        /** Sets the value as a user would; false = not handled (the generic input + change events). */
        default boolean setValue(WebElement element, Object value, SeleniumSession session) {
            return false;
        }

        /** A component-specific access object for the core (e.g. GridAccess), reached through unwrap. */
        default Optional<Object> unwrap(Class<?> type, WebElement element, SeleniumSession session) {
            return Optional.empty();
        }
    }

    static final Support DEFAULT = new Support() {};
    private static final Map<Class<? extends Component>, Support> REGISTRY = new LinkedHashMap<>();

    static {
        register(TextField.class, new Support() {
            @Override
            public boolean setValue(WebElement element, Object value, SeleniumSession session) {
                session.testBench(element).wrap(TextFieldElement.class).setValue(value == null ? "" : value.toString());
                return true;
            }
        });
        register(NumberField.class, new Support() {
            @Override
            public boolean setValue(WebElement element, Object value, SeleniumSession session) {
                String text = value == null ? ""
                        : new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
                session.testBench(element).wrap(NumberFieldElement.class).setValue(text);
                return true;
            }
        });
        register(DatePicker.class, new Support() {
            @Override
            public boolean setValue(WebElement element, Object value, SeleniumSession session) {
                DatePickerElement picker = session.testBench(element).wrap(DatePickerElement.class);
                if (value == null) {
                    picker.clear();
                } else {
                    picker.setDate((LocalDate) value);
                }
                return true;
            }
        });
        register(Checkbox.class, new Support() {
            @Override
            public boolean setValue(WebElement element, Object value, SeleniumSession session) {
                session.testBench(element).wrap(CheckboxElement.class).setChecked(Boolean.TRUE.equals(value));
                return true;
            }
        });
    }

    static {
        register(Notification.class, new Support() {
            @Override
            public String selector(String componentTag) {
                // the visible part is the open card, not <vaadin-notification>
                return "vaadin-notification-card[slot]";
            }

            @Override
            public Optional<Boolean> booleanProperty(WebElement element, String name, SeleniumSession session) {
                return "opened".equals(name) ? Optional.of(element.isDisplayed()) : Optional.empty();
            }
        });
        register(Dialog.class, new Support() {
            @Override
            public Optional<Boolean> visible(WebElement element, SeleniumSession session) {
                // the dialog host has no box of its own; open means shown
                return Optional.of(Boolean.TRUE.equals(session.js("return !!arguments[0].opened;", element)));
            }
        });
    }

    static {
        register(Grid.class, access(GridAccess.class, (element, session) -> new SeleniumGridAccess(session, element)));
        register(ComboBox.class, access(SelectionAccess.class,
                (element, session) -> new SeleniumSelectionAccess(session, element, SeleniumSelectionAccess.Kind.COMBO_BOX)));
        register(MultiSelectComboBox.class, access(SelectionAccess.class, (element, session) ->
                new SeleniumSelectionAccess(session, element, SeleniumSelectionAccess.Kind.MULTI_SELECT_COMBO_BOX)));
        register(RadioButtonGroup.class, access(SelectionAccess.class, (element, session) ->
                new SeleniumSelectionAccess(session, element, SeleniumSelectionAccess.Kind.RADIO_BUTTON_GROUP)));
        register(TabSheet.class, access(TabSheetAccess.class, (element, session) -> new SeleniumTabSheetAccess(session, element)));
    }

    private static Support access(Class<?> accessType,
            java.util.function.BiFunction<WebElement, SeleniumSession, Object> factory) {
        return new Support() {
            @Override
            public Optional<Object> unwrap(Class<?> type, WebElement element, SeleniumSession session) {
                return type == accessType ? Optional.of(factory.apply(element, session)) : Optional.empty();
            }
        };
    }

    static void register(Class<? extends Component> type, Support support) {
        REGISTRY.put(type, support);
    }

    /** The support for the most specific registered supertype of {@code type}, or the generic default. */
    static Support of(Class<?> type) {
        Support found = DEFAULT;
        Class<?> best = null;
        for (Map.Entry<Class<? extends Component>, Support> e : REGISTRY.entrySet()) {
            if (e.getKey().isAssignableFrom(type) && (best == null || best.isAssignableFrom(e.getKey()))) {
                best = e.getKey();
                found = e.getValue();
            }
        }
        return found;
    }
}
