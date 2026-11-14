package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import java.util.List;
import java.util.Optional;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.VaadinElement;
import org.vaadin.addons.dramafinder.element.shared.FocusableElement;
import org.vaadin.addons.dramafinder.element.shared.HasDisabledAttributeElement;
import org.vaadin.addons.dramafinder.element.shared.HasValueElement;

/**
 * Unified Element on Playwright: a locator for one component, with its Dramafinder element when the
 * component has one. Reads and actions go through Dramafinder: the component's own element where it exists,
 * else a generic Dramafinder element on the locator. Dramafinder's actions wait for the server round-trip;
 * actions on a raw locator wait here.
 * <p>
 * The scripts left here have no Dramafinder API yet; each names the Dramafinder issue that would replace it.
 */
final class PlaywrightElement implements UnifiedElement {

    private final PlaywrightSession session;
    private final Locator locator;
    private final VaadinElement dramafinder;
    /** The generic Dramafinder element on the same locator: what every component has. */
    private final GenericElement generic;
    private final PlaywrightComponents.Support support;

    PlaywrightElement(PlaywrightSession session, Locator locator, VaadinElement dramafinder,
            PlaywrightComponents.Support support) {
        this.session = session;
        this.locator = locator;
        this.dramafinder = dramafinder;
        this.generic = new GenericElement(locator);
        this.support = support;
    }

    /**
     * A Dramafinder element for any component: the generic {@code VaadinElement} API, focus, and enabled
     * state from the {@code disabled} attribute. To be replaced by Dramafinder's own factory
     * (parttio/dramafinder#269).
     */
    private static final class GenericElement extends VaadinElement
            implements FocusableElement, HasDisabledAttributeElement {
        GenericElement(Locator locator) {
            super(locator);
        }
    }

    Locator locator() {
        return locator;
    }

    private Page page() {
        return session.page();
    }

    // ---------------------------------------------------------------- reading

    @Override
    public String getTag() {
        return (String) locator.evaluate("e => e.localName"); // parttio/dramafinder#270
    }

    /** The DOM text ({@code textContent}), whitespace collapsed: what Browserless reads on the server. */
    @Override
    public String getText() {
        Optional<String> special = support.text(locator);
        return BrowserQueries.normalize(special.orElseGet(generic::getText));
    }

    @Override
    public String getVisibleText() {
        return locator.innerText().strip();
    }

    @Override
    public String getInlineStyle(String name) {
        // parttio/dramafinder#271
        return (String) locator.evaluate(BrowserQueries.INLINE_STYLE, BrowserQueries.cssName(name));
    }

    @Override
    public String getProperty(String name) {
        Object value = generic.getProperty(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            return String.valueOf(n.longValue());
        }
        return String.valueOf(value);
    }

    @Override
    public Integer getPropertyInteger(String name) {
        String value = getProperty(name);
        return value == null ? null : (int) Double.parseDouble(value);
    }

    /** The property's JavaScript truthiness. */
    @Override
    public Boolean getPropertyBoolean(String name) {
        Optional<Boolean> special = support.booleanProperty(locator, name);
        if (special.isPresent()) {
            return special.get();
        }
        Object value = generic.getProperty(name);
        if (value == null || value instanceof Boolean b && !b) {
            return false;
        }
        if (value instanceof Number n) {
            double d = n.doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        return !(value instanceof String s && s.isEmpty());
    }

    @Override
    public String getAttribute(String name) {
        return locator.getAttribute(name);
    }

    @Override
    public String getCssValue(String property) {
        // parttio/dramafinder#271
        return (String) locator.evaluate("(e, p) => getComputedStyle(e).getPropertyValue(p)", property);
    }

    @Override
    public boolean isVisible() {
        return support.visible(locator).orElseGet(locator::isVisible);
    }

    /** No {@code disabled} attribute: Vaadin components reflect the property to it. */
    @Override
    public boolean isEnabled() {
        return generic.isEnabled();
    }

    @Override
    public List<UnifiedElement> findChildren(String tag) {
        return lightDom(BrowserQueries.lightDomXPath(tag, false));
    }

    @Override
    public List<UnifiedElement> findDescendants(String tag) {
        return lightDom(BrowserQueries.lightDomXPath(tag, true));
    }

    /** Light DOM only: CSS would pierce shadow roots. parttio/dramafinder#272 */
    private List<UnifiedElement> lightDom(String xpath) {
        return locator.locator("xpath=" + xpath).all().stream()
                .map(l -> (UnifiedElement) new PlaywrightElement(session, l, null, PlaywrightComponents.DEFAULT)).toList();
    }

    // ---------------------------------------------------------------- acting

    @Override
    public void setProperty(String name, Object value) {
        generic.setProperty(name, value);
    }

    @Override
    public void click() {
        (dramafinder != null ? dramafinder : generic).click();
    }

    @Override
    public void setValueAsUser(Object value) {
        if (support.setValue(locator, value)) {
            return; // Dramafinder's element set it and waited
        }
        String text = BrowserQueries.typedText(value);
        if (dramafinder instanceof HasValueElement field) {
            field.setValue(text);
            return;
        }
        // the element itself when it is the native input, else the input inside it
        boolean isInput = Boolean.TRUE.equals(
                locator.evaluate("e => e.matches('input, textarea')")); // parttio/dramafinder#270
        Locator input = isInput ? locator : locator.locator("input, textarea").first();
        input.fill(text); // fires input
        input.dispatchEvent("change");
        AutoWait.afterAction(page());
    }

    @Override
    public void focus() {
        focusable().focus();
    }

    @Override
    public void blur() {
        focusable().blur();
    }

    @Override
    public void pressKey(String key) {
        focusable().getFocusLocator().press(key);
        AutoWait.afterAction(page());
    }

    @Override
    public void dispatchEvent(String eventName) {
        locator.dispatchEvent(eventName);
        AutoWait.afterAction(page());
    }

    /**
     * Dramafinder's focus handling: a field's input, not its host element, which can't be focused (keys
     * pressed on it would go to the page body).
     */
    private FocusableElement focusable() {
        return dramafinder instanceof FocusableElement focusable ? focusable : generic;
    }

    // ---------------------------------------------------------------- Escape Hatch

    @Override
    public <T> T unwrap(Class<T> type) {
        return tryUnwrap(type).orElseThrow();
    }

    @Override
    public <T> Optional<T> tryUnwrap(Class<T> type) {
        Optional<Object> special = support.unwrap(type, locator, session);
        if (special.isPresent()) {
            return special.map(type::cast);
        }
        if (type.isInstance(locator)) {
            return Optional.of(type.cast(locator));
        }
        if (dramafinder != null && type.isInstance(dramafinder)) {
            return Optional.of(type.cast(dramafinder));
        }
        if (VaadinElement.class.isAssignableFrom(type)) {
            try {
                return Optional.of(type.getConstructor(Locator.class).newInstance(locator));
            } catch (ReflectiveOperationException e) {
                return Optional.empty();
            }
        }
        if (type.isInstance(page())) {
            return Optional.of(type.cast(page()));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "PlaywrightElement[" + locator + "]";
    }
}
