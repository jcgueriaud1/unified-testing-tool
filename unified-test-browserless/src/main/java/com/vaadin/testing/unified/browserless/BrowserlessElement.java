package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.testing.unified.UnifiedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Unified Element on Browserless: reads the server-side element and component; user actions go through
 * the component's tester (events fire as from the browser), then a round-trip.
 */
final class BrowserlessElement implements UnifiedElement {

    private final BrowserlessSession session;
    private final Component component;
    private final BrowserlessComponents.Support support;
    private final List<Component> scopeRoots;

    BrowserlessElement(BrowserlessSession session, Component component) {
        this(session, component, List.of(component));
    }

    /** An element whose scope is several components (e.g. a grid column's header cells). */
    BrowserlessElement(BrowserlessSession session, Component component, List<Component> scopeRoots) {
        this.session = session;
        this.component = component;
        this.support = BrowserlessComponents.of(component.getClass());
        this.scopeRoots = scopeRoots;
    }

    /** Where queries scoped to this element search: these components and their descendants. */
    List<Component> scopeRoots() {
        return scopeRoots;
    }

    Component component() {
        return component;
    }

    private Element element() {
        return component.getElement();
    }

    private ComponentTester<?> tester() {
        return session.window().test(component);
    }

    // ---------------------------------------------------------------- reading

    @Override
    public String getTag() {
        return element().getTag();
    }

    @Override
    public String getText() {
        return support.text(component, session.window()).orElseGet(() -> element().getTextRecursively());
    }

    @Override
    public String getProperty(String name) {
        if ("value".equals(name) && component instanceof HasValue<?, ?> hasValue) {
            Object value = hasValue.getValue();
            return value == null ? null : String.valueOf(value);
        }
        if (element().hasProperty(name)) {
            Object value = element().getPropertyRaw(name);
            return value == null ? null : String.valueOf(value);
        }
        return element().getAttribute(name);
    }

    @Override
    public Integer getPropertyInteger(String name) {
        String value = getProperty(name);
        return value == null ? null : (int) Double.parseDouble(value);
    }

    @Override
    public Boolean getPropertyBoolean(String name) {
        Optional<Boolean> special = support.booleanProperty(component, name);
        if (special.isPresent()) {
            return special.get();
        }
        if (element().hasProperty(name)) {
            return element().getProperty(name, false);
        }
        if (element().hasAttribute(name)) {
            return true;
        }
        return Boolean.FALSE;
    }

    @Override
    public String getAttribute(String name) {
        return element().getAttribute(name);
    }

    @Override
    public String getCssValue(String property) {
        throw new UnsupportedOperationException("Browserless has no CSS; use @RequiresBrowser");
    }

    @Override
    public boolean isVisible() {
        Optional<Boolean> special = support.visible(component);
        if (special.isPresent()) {
            return special.get();
        }
        if (!component.isAttached()) {
            return false;
        }
        for (Component c = component; c != null && !(c instanceof UI); c = c.getParent().orElse(null)) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEnabled() {
        return element().isEnabled();
    }

    @Override
    public List<UnifiedElement> findChildren(String tag) {
        return component.getChildren().filter(c -> tag.equals(c.getElement().getTag()))
                .map(c -> (UnifiedElement) new BrowserlessElement(session, c)).toList();
    }

    // ---------------------------------------------------------------- acting

    @Override
    public void setProperty(String name, Object value) {
        if (value instanceof Boolean b) {
            element().setProperty(name, b);
        } else if (value instanceof Number n) {
            element().setProperty(name, n.doubleValue());
        } else {
            element().setProperty(name, value == null ? null : String.valueOf(value));
        }
        session.window().roundTrip();
    }

    @Override
    public void click() {
        if (hasTesterMethod("click", 0)) {
            invokeTester("click");
        } else {
            ComponentUtil.fireEvent(component, new ClickEvent<>(component));
        }
        session.window().roundTrip();
    }

    @Override
    public void setValueAsUser(Object value) {
        Method setValue = testerMethod("setValue", 1)
                .orElseThrow(() -> new UnsupportedOperationException(
                        component.getClass().getSimpleName() + " has no value to set on Browserless"));
        call(setValue, value);
        session.window().roundTrip();
    }

    @Override
    public void focus() {
        if (component instanceof Focusable<?> focusable) {
            focusable.focus();
        }
        session.window().roundTrip();
    }

    @Override
    public void blur() {
        if (component instanceof Focusable<?> focusable) {
            focusable.blur();
        }
        session.window().roundTrip();
    }

    @Override
    public void pressKey(String key) {
        if (!support.pressKey(component, key, session.window())) {
            throw new UnsupportedOperationException("pressKey(\"" + key + "\") on "
                    + component.getClass().getSimpleName() + " is not implemented on Browserless yet");
        }
        session.window().roundTrip();
    }

    @Override
    public void dispatchEvent(String eventName) {
        throw new UnsupportedOperationException("dispatchEvent is not implemented on Browserless yet (first version)");
    }

    // ---------------------------------------------------------------- Escape Hatch

    @Override
    public <T> T unwrap(Class<T> type) {
        return tryUnwrap(type).orElseThrow();
    }

    @Override
    public <T> Optional<T> tryUnwrap(Class<T> type) {
        Optional<Object> special = support.unwrap(type, component, session);
        if (special.isPresent()) {
            return special.map(type::cast);
        }
        if (type.isInstance(component)) {
            return Optional.of(type.cast(component));
        }
        if (ComponentTester.class.isAssignableFrom(type)) {
            ComponentTester<?> tester = tester();
            return type.isInstance(tester) ? Optional.of(type.cast(tester)) : Optional.empty();
        }
        if (type.isInstance(element())) {
            return Optional.of(type.cast(element()));
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- helpers

    /** What a screen reader announces, approximated: aria-label, aria-labelledby target, label, text. */
    static String accessibleName(Component c) {
        Element e = c.getElement();
        String ariaLabel = e.getAttribute("aria-label");
        if (ariaLabel != null && !ariaLabel.isBlank()) {
            return ariaLabel;
        }
        String labelledBy = e.getAttribute("aria-labelledby");
        if (labelledBy != null && !labelledBy.isBlank()) {
            Optional<UI> ui = c.getUI();
            if (ui.isPresent()) {
                Optional<String> text = ui.get().getElement().getChildren()
                        .flatMap(BrowserlessElement::descendants)
                        .filter(el -> labelledBy.equals(el.getAttribute("id")))
                        .map(Element::getTextRecursively).findFirst();
                if (text.isPresent()) {
                    return text.get();
                }
            }
        }
        String label = e.getProperty("label");
        if (label != null && !label.isBlank()) {
            return label;
        }
        return e.getTextRecursively();
    }

    private static java.util.stream.Stream<Element> descendants(Element e) {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(e), e.getChildren().flatMap(BrowserlessElement::descendants));
    }

    private boolean hasTesterMethod(String name, int params) {
        return testerMethod(name, params).isPresent();
    }

    private Optional<Method> testerMethod(String name, int params) {
        for (Method m : tester().getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == params) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    private Object invokeTester(String name) {
        return call(testerMethod(name, 0).orElseThrow(
                () -> new UnsupportedOperationException(tester().getClass().getSimpleName() + " has no " + name + "()")));
    }

    private Object call(Method method, Object... args) {
        try {
            return method.invoke(tester(), args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "BrowserlessElement[" + component.getClass().getSimpleName() + "]";
    }
}
