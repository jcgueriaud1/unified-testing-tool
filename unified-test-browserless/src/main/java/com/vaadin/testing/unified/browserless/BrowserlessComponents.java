package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.Component;
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

        /** Whether Browserless's own text query sees this component's text. */
        default boolean queryFindsText() {
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

            @Override
            public boolean queryFindsText() {
                return false;
            }
        });
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
