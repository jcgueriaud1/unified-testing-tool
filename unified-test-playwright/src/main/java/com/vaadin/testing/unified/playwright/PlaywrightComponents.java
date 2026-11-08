package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
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

        /** Visible text, when it is not the element's own text. */
        default Optional<String> text(Locator locator) {
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
        register(Notification.class, new Support() {
            @Override
            public String selector(String componentTag) {
                // the visible part is the open card, not <vaadin-notification>
                return "vaadin-notification-card[slot]";
            }

            @Override
            public Optional<String> text(Locator locator) {
                return Optional.of(new NotificationElement(locator).getText());
            }

            @Override
            public Optional<Boolean> booleanProperty(Locator locator, String name) {
                return "opened".equals(name) ? Optional.of(locator.isVisible()) : Optional.empty();
            }
        });
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
