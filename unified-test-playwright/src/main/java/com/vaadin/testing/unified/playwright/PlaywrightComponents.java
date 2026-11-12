package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import com.vaadin.testing.unified.spi.SelectionAccess;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.testing.unified.spi.GridAccess;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.CheckboxElement;
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
import org.vaadin.addons.dramafinder.element.MultiSelectComboBoxElement;
import org.vaadin.addons.dramafinder.element.RadioButtonGroupElement;
import org.vaadin.addons.dramafinder.element.TabSheetElement;
import org.vaadin.addons.dramafinder.element.DatePickerElement;
import org.vaadin.addons.dramafinder.element.DialogElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.NumberFieldElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;
import org.vaadin.addons.dramafinder.element.VaadinElement;

/**
 * The Playwright adapter's Core Set registrations: which Dramafinder element finds a component, which tag
 * stands for it on the page, and how it is read when the generic Unified Element behaviour is not enough.
 * Adding a Core Set component means one entry here.
 */
final class PlaywrightComponents {
    private PlaywrightComponents() {}

    /** How the Playwright adapter handles one component type. Every method has a generic default. */
    interface Support {
        /** Dramafinder element whose query builder finds this component; null = plain locators on the tag. */
        default Class<? extends VaadinElement> dramafinderElement() {
            return null;
        }

        /** CSS selector for the component on the page; by default its web component tag. */
        default String selector(String componentTag) {
            return componentTag;
        }

        /** Whether a found element counts as a match at all (e.g. only open dialogs). */
        default boolean include(Locator locator) {
            return true;
        }

        /** Visible text, when it is not the element's own text. */
        default Optional<String> text(Locator locator) {
            return Optional.empty();
        }

        /** Visibility, when it is not the element's own (e.g. an overlay). */
        default Optional<Boolean> visible(Locator locator) {
            return Optional.empty();
        }

        /** Sets the value as a user would, when typing its text is not enough; false = not handled. */
        default boolean setValue(Locator locator, Object value) {
            return false;
        }

        /** A component-specific access object for the core (e.g. GridAccess), reached through unwrap. */
        default Optional<Object> unwrap(Class<?> type, Locator locator, PlaywrightSession session) {
            return Optional.empty();
        }

        /** A boolean property computed in another way than reading the DOM property. */
        default Optional<Boolean> booleanProperty(Locator locator, String name) {
            return Optional.empty();
        }
    }

    static final Support DEFAULT = new Support() {};
    private static final Map<Class<? extends Component>, Support> REGISTRY = new LinkedHashMap<>();

    static {
        register(TextField.class, dramafinder(TextFieldElement.class));
        register(Button.class, dramafinder(ButtonElement.class));
        register(NumberField.class, dramafinder(NumberFieldElement.class));
        register(DatePicker.class, new Support() {
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return DatePickerElement.class;
            }

            @Override
            public boolean setValue(Locator locator, Object value) {
                DatePickerElement picker = new DatePickerElement(locator);
                if (value instanceof String text) { // UnifiedDatePicker.typeText: typed as it is, then Enter
                    picker.setValue(text);
                    return true;
                }
                if (value == null) {
                    picker.clear();
                } else {
                    picker.setValue((LocalDate) value);
                }
                return true;
            }
        });
        register(Checkbox.class, new Support() {
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return CheckboxElement.class;
            }

            @Override
            public boolean setValue(Locator locator, Object value) {
                new CheckboxElement(locator).setChecked(Boolean.TRUE.equals(value));
                return true;
            }
        });
        register(Notification.class, new Support() {
            /** Dramafinder's query finds the open notification cards, what the user sees. */
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return NotificationElement.class;
            }

            @Override
            public Optional<Boolean> booleanProperty(Locator locator, String name) {
                return "opened".equals(name) ? Optional.of(new NotificationElement(locator).isOpen()) : Optional.empty();
            }
        });
    }

    static {
        register(Dialog.class, new Support() {
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return DialogElement.class;
            }

            @Override
            public Optional<Boolean> visible(Locator locator) {
                // the dialog host has no box; its overlay is what the user sees
                return Optional.of(new DialogElement(locator).isVisible());
            }
        });
    }

    static {
        register(ComboBox.class, selection(ComboBoxElement.class, PlaywrightSelectionAccess.Kind.COMBO_BOX));
        register(MultiSelectComboBox.class, selection(MultiSelectComboBoxElement.class,
                PlaywrightSelectionAccess.Kind.MULTI_SELECT_COMBO_BOX));
        register(RadioButtonGroup.class, selection(RadioButtonGroupElement.class,
                PlaywrightSelectionAccess.Kind.RADIO_BUTTON_GROUP));
    }

    private static Support selection(Class<? extends VaadinElement> element, PlaywrightSelectionAccess.Kind kind) {
        return new Support() {
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return element;
            }

            @Override
            public Optional<Object> unwrap(Class<?> type, Locator locator, PlaywrightSession session) {
                return type == SelectionAccess.class
                        ? Optional.of(new PlaywrightSelectionAccess(session, locator, kind))
                        : Optional.empty();
            }
        };
    }

    static {
        register(TabSheet.class, new Support() {
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return TabSheetElement.class;
            }

            @Override
            public Optional<Object> unwrap(Class<?> type, Locator locator, PlaywrightSession session) {
                return type == TabSheetAccess.class
                        ? Optional.of(new PlaywrightTabSheetAccess(session, locator))
                        : Optional.empty();
            }
        });
    }

    static {
        register(Grid.class, new Support() {
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return GridElement.class;
            }

            @Override
            public Optional<Object> unwrap(Class<?> type, Locator locator, PlaywrightSession session) {
                return type == GridAccess.class
                        ? Optional.of(new PlaywrightGridAccess(session, locator))
                        : Optional.empty();
            }
        });
    }

    static {
        register(com.vaadin.flow.component.textfield.TextArea.class,
                dramafinder(org.vaadin.addons.dramafinder.element.TextAreaElement.class));
        register(com.vaadin.flow.component.textfield.IntegerField.class,
                dramafinder(org.vaadin.addons.dramafinder.element.IntegerFieldElement.class));
        register(com.vaadin.flow.component.textfield.BigDecimalField.class,
                dramafinder(org.vaadin.addons.dramafinder.element.BigDecimalFieldElement.class));
        register(com.vaadin.flow.component.datetimepicker.DateTimePicker.class, new Support() {
            @Override
            public Optional<Object> unwrap(Class<?> type, Locator locator, PlaywrightSession session) {
                return type == com.vaadin.testing.unified.spi.DateTimePickerAccess.class
                        ? Optional.of(new PlaywrightCoreSetAccess.DateTimePickerAccessImpl(session, locator))
                        : Optional.empty();
            }

            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return org.vaadin.addons.dramafinder.element.DateTimePickerElement.class;
            }

            @Override
            public boolean setValue(Locator locator, Object value) {
                org.vaadin.addons.dramafinder.element.DateTimePickerElement picker =
                        new org.vaadin.addons.dramafinder.element.DateTimePickerElement(locator);
                if (value == null) {
                    picker.clear(); // both halves
                } else {
                    picker.setValue((java.time.LocalDateTime) value);
                }
                return true;
            }
        });
        register(com.vaadin.flow.component.tabs.Tabs.class, access(TabSheetAccess.class,
                PlaywrightCoreSetAccess.TabsAccess::new));
        register(com.vaadin.flow.component.details.Details.class, access(com.vaadin.testing.unified.spi.DetailsAccess.class,
                PlaywrightCoreSetAccess.DetailsAccessImpl::new));
        register(com.vaadin.flow.component.menubar.MenuBar.class, access(com.vaadin.testing.unified.spi.MenuBarAccess.class,
                PlaywrightCoreSetAccess.MenuBarAccessImpl::new));
        register(com.vaadin.flow.component.upload.Upload.class, access(com.vaadin.testing.unified.spi.UploadAccess.class,
                PlaywrightCoreSetAccess.UploadAccessImpl::new));
    }

    /** A support on plain locators whose job is giving the core an access object of this type. */
    private static Support access(Class<?> accessType,
            java.util.function.BiFunction<PlaywrightSession, Locator, Object> factory) {
        return new Support() {
            @Override
            public Optional<Object> unwrap(Class<?> type, Locator locator, PlaywrightSession session) {
                return type == accessType ? Optional.of(factory.apply(session, locator)) : Optional.empty();
            }
        };
    }

    static void register(Class<? extends Component> type, Support support) {
        REGISTRY.put(type, support);
    }

    static Support dramafinder(Class<? extends VaadinElement> element) {
        return new Support() {
            @Override
            public Class<? extends VaadinElement> dramafinderElement() {
                return element;
            }
        };
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
