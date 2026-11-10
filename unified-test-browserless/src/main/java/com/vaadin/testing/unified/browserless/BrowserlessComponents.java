package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import com.vaadin.testing.unified.spi.SelectionAccess;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.testing.unified.spi.GridAccess;
import com.vaadin.flow.component.notification.Notification;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The Browserless adapter's Core Set registrations: how each component type is read and found when the
 * generic Unified Element behaviour is not enough. Adding a Core Set component means one entry here.
 */
final class BrowserlessComponents {
    private BrowserlessComponents() {}

    /** How the Browserless adapter handles one component type. Every method has a generic default. */
    interface Support {
        /** Visible text, when it is not the element's text (e.g. rendered content). */
        default Optional<String> text(Component component, BrowserlessUIContext window) {
            return Optional.empty();
        }

        /** A boolean property computed from the component instead of the element. */
        default Optional<Boolean> booleanProperty(Component component, String name) {
            return Optional.empty();
        }

        /** Visibility, when it is not the component tree's visibility. */
        default Optional<Boolean> visible(Component component) {
            return Optional.empty();
        }

        /** Sets the value as a user would, when the tester has no {@code setValue}; false = not handled. */
        default boolean setValue(Component component, Object value, BrowserlessUIContext window) {
            return false;
        }

        /** Handles a key press on the component; false if this component has no key handling. */
        default boolean pressKey(Component component, String key, BrowserlessUIContext window) {
            return false;
        }

        /** A component-specific access object for the core (e.g. GridAccess), reached through unwrap. */
        default Optional<Object> unwrap(Class<?> type, Component component, BrowserlessSession session) {
            return Optional.empty();
        }

        /** Whether a found component counts as a match at all (e.g. only open dialogs). */
        default boolean include(Component component) {
            return true;
        }
    }

    private static final Support DEFAULT = new Support() {};
    private static final Map<Class<? extends Component>, Support> REGISTRY = new LinkedHashMap<>();

    static {
        register(Notification.class, new Support() {
            @Override
            public Optional<String> text(Component component, BrowserlessUIContext window) {
                // the text is in a renderer: read it through the tester
                ComponentTester<?> tester = window.test(component);
                try {
                    return Optional.of((String) tester.getClass().getMethod("getText").invoke(tester));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("NotificationTester.getText() not available", e);
                }
            }

            @Override
            public Optional<Boolean> booleanProperty(Component component, String name) {
                return "opened".equals(name) ? Optional.of(((Notification) component).isOpened()) : Optional.empty();
            }

            @Override
            public Optional<Boolean> visible(Component component) {
                return Optional.of(((Notification) component).isOpened());
            }

            /** Open notifications only: a browser adapter finds the open card. */
            @Override
            public boolean include(Component component) {
                return ((Notification) component).isOpened();
            }
        });
    }

    static {
        register(Dialog.class, new Support() {
            @Override
            public Optional<Boolean> booleanProperty(Component component, String name) {
                return "opened".equals(name) ? Optional.of(((Dialog) component).isOpened()) : Optional.empty();
            }

            @Override
            public Optional<Boolean> visible(Component component) {
                return Optional.of(((Dialog) component).isOpened());
            }

            /** Open dialogs only, as in a browser adapter: a closed dialog is not part of the page. */
            @Override
            public boolean include(Component component) {
                return ((Dialog) component).isOpened();
            }

            @Override
            public boolean pressKey(Component component, String key, BrowserlessUIContext window) {
                if (!"Escape".equals(key)) {
                    return false;
                }
                invoke(window.test(component), "pressEscape");
                return true;
            }
        });
    }

    static {
        register(Checkbox.class, new Support() {
            @Override
            public boolean setValue(Component component, Object value, BrowserlessUIContext window) {
                // CheckboxTester has check() and uncheck() instead of setValue
                boolean checked = Boolean.TRUE.equals(value);
                if (((Checkbox) component).getValue() != checked) {
                    invoke(window.test(component), checked ? "check" : "uncheck");
                }
                return true;
            }
        });
    }

    static {
        Support selection = new Support() {
            @Override
            public Optional<Object> unwrap(Class<?> type, Component component, BrowserlessSession session) {
                return type == SelectionAccess.class
                        ? Optional.of(new BrowserlessSelectionAccess(session, component))
                        : Optional.empty();
            }
        };
        register(ComboBox.class, selection);
        register(MultiSelectComboBox.class, selection);
        register(RadioButtonGroup.class, selection);
    }

    static {
        register(TabSheet.class, new Support() {
            @Override
            public Optional<Object> unwrap(Class<?> type, Component component, BrowserlessSession session) {
                return type == TabSheetAccess.class
                        ? Optional.of(new BrowserlessTabSheetAccess(session, (TabSheet) component))
                        : Optional.empty();
            }
        });
    }

    static {
        register(com.vaadin.flow.component.tabs.Tabs.class, access(TabSheetAccess.class, (session, c) ->
                new BrowserlessCoreSetAccess.TabsAccess(session, (com.vaadin.flow.component.tabs.Tabs) c)));
        register(com.vaadin.flow.component.details.Details.class, access(com.vaadin.testing.unified.spi.DetailsAccess.class,
                (session, c) -> new BrowserlessCoreSetAccess.DetailsAccessImpl(session,
                        (com.vaadin.flow.component.details.Details) c)));
        register(com.vaadin.flow.component.menubar.MenuBar.class, access(com.vaadin.testing.unified.spi.MenuBarAccess.class,
                (session, c) -> new BrowserlessCoreSetAccess.MenuBarAccessImpl(session,
                        (com.vaadin.flow.component.menubar.MenuBar) c)));
        register(com.vaadin.flow.component.upload.Upload.class, access(com.vaadin.testing.unified.spi.UploadAccess.class,
                (session, c) -> new BrowserlessCoreSetAccess.UploadAccessImpl(session,
                        (com.vaadin.flow.component.upload.Upload) c)));
        register(com.vaadin.flow.component.datepicker.DatePicker.class, new Support() {
            /** A String: text typed as it is (UnifiedDatePicker.typeText); ISO dates are parsed. */
            @Override
            public boolean setValue(Component component, Object value, BrowserlessUIContext window) {
                if (!(value instanceof String text)) {
                    return false;
                }
                com.vaadin.flow.component.datepicker.DatePicker picker = (com.vaadin.flow.component.datepicker.DatePicker) component;
                java.time.LocalDate date = null;
                try {
                    date = text.isBlank() ? null : java.time.LocalDate.parse(text.strip());
                } catch (java.time.format.DateTimeParseException e) {
                    // not a date: what a browser does with text it can't parse, it reports an unparsable change
                    picker.getElement().setProperty("_inputElementValue", text);
                    com.vaadin.browserless.internal.BasicUtilsKt._fireDomEvent(component, "unparsable-change");
                    return true;
                }
                Object tester = window.test(component);
                try {
                    if (date == null) {
                        tester.getClass().getMethod("clear").invoke(tester);
                    } else {
                        tester.getClass().getMethod("setValue", java.time.LocalDate.class).invoke(tester, date);
                    }
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause() instanceof RuntimeException r ? r : new IllegalStateException(e.getCause());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                return true;
            }
        });
        register(Grid.class, new Support() {
            @Override
            public Optional<Object> unwrap(Class<?> type, Component component, BrowserlessSession session) {
                return type == GridAccess.class
                        ? Optional.of(new BrowserlessGridAccess(session, (Grid<?>) component))
                        : Optional.empty();
            }
        });
    }

    /** A support whose only job is giving the core an access object of this type. */
    private static Support access(Class<?> accessType,
            java.util.function.BiFunction<BrowserlessSession, Component, Object> factory) {
        return new Support() {
            @Override
            public Optional<Object> unwrap(Class<?> type, Component component, BrowserlessSession session) {
                return type == accessType ? Optional.of(factory.apply(session, component)) : Optional.empty();
            }
        };
    }

    private static Object invoke(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause() instanceof RuntimeException r ? r : new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(target.getClass().getSimpleName() + "." + method + "() not available", e);
        }
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
