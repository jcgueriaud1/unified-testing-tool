package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.spi.Filter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.VaadinElement;
import org.vaadin.addons.dramafinder.element.shared.FocusableElement;
import org.vaadin.addons.dramafinder.element.shared.HasValueElement;

/**
 * Unified Element on Playwright: a locator for one component, with its Dramafinder element when the
 * component has one. Actions use Dramafinder where it exists (it waits for the server round-trip), else
 * Playwright, then wait for Vaadin to be idle.
 */
final class PlaywrightElement implements UnifiedElement {

    private final PlaywrightSession session;
    private final Locator locator;
    private final VaadinElement dramafinder;
    private final PlaywrightComponents.Support support;

    PlaywrightElement(PlaywrightSession session, Locator locator, VaadinElement dramafinder,
            PlaywrightComponents.Support support) {
        this.session = session;
        this.locator = locator;
        this.dramafinder = dramafinder;
        this.support = support;
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
        return (String) locator.evaluate("e => e.localName");
    }

    /** The DOM text ({@code textContent}), whitespace collapsed: what Browserless reads on the server. */
    @Override
    public String getText() {
        Optional<String> special = support.text(locator);
        if (special.isPresent()) {
            return BrowserQueries.normalize(special.get());
        }
        return (String) locator.evaluate(BrowserQueries.TEXT);
    }

    @Override
    public String getVisibleText() {
        return locator.innerText().strip();
    }

    @Override
    public String getInlineStyle(String name) {
        return (String) locator.evaluate(BrowserQueries.INLINE_STYLE, BrowserQueries.cssName(name));
    }

    @Override
    public String getProperty(String name) {
        Object value = locator.evaluate("(e, n) => e[n]", name);
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

    @Override
    public Boolean getPropertyBoolean(String name) {
        Optional<Boolean> special = support.booleanProperty(locator, name);
        if (special.isPresent()) {
            return special.get();
        }
        return Boolean.TRUE.equals(locator.evaluate("(e, n) => !!e[n]", name));
    }

    @Override
    public String getAttribute(String name) {
        return locator.getAttribute(name);
    }

    @Override
    public String getCssValue(String property) {
        return (String) locator.evaluate("(e, p) => getComputedStyle(e).getPropertyValue(p)", property);
    }

    @Override
    public boolean isVisible() {
        return support.visible(locator).orElseGet(locator::isVisible);
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(locator.evaluate("e => !e.disabled && !e.hasAttribute('disabled')"));
    }

    @Override
    public List<UnifiedElement> findChildren(String tag) {
        return locator.locator(tag).all().stream()
                .map(l -> (UnifiedElement) new PlaywrightElement(session, l, null, PlaywrightComponents.DEFAULT)).toList();
    }

    // ---------------------------------------------------------------- acting

    @Override
    public void setProperty(String name, Object value) {
        locator.evaluate("(e, a) => { e[a.name] = a.value; }", java.util.Map.of("name", name, "value", value));
        AutoWait.afterAction(page());
    }

    @Override
    public void click() {
        if (dramafinder != null) {
            dramafinder.click();
        } else {
            locator.click();
        }
        AutoWait.afterAction(page());
    }

    @Override
    public void setValueAsUser(Object value) {
        if (support.setValue(locator, value)) {
            AutoWait.afterAction(page());
            return;
        }
        String text = value == null ? ""
                : value instanceof Number n ? new BigDecimal(n.toString()).stripTrailingZeros().toPlainString()
                : String.valueOf(value);
        if (dramafinder instanceof HasValueElement field) {
            field.setValue(text);
        } else {
            // the element itself when it is the native input, else the input inside it
            boolean isInput = Boolean.TRUE.equals(locator.evaluate("e => e.matches('input, textarea')"));
            Locator input = isInput ? locator : locator.locator("input, textarea").first();
            input.fill(text); // fires input
            input.dispatchEvent("change");
        }
        AutoWait.afterAction(page());
    }

    @Override
    public void focus() {
        focusTarget().focus();
        AutoWait.afterAction(page());
    }

    @Override
    public void blur() {
        locator.evaluate("e => e.blur()");
        AutoWait.afterAction(page());
    }

    @Override
    public void pressKey(String key) {
        focusTarget().press(key);
        AutoWait.afterAction(page());
    }

    @Override
    public void dispatchEvent(String eventName) {
        locator.dispatchEvent(eventName);
        AutoWait.afterAction(page());
    }

    /**
     * The part that takes keyboard focus: a field's input, not its host element, which can't be focused
     * (keys pressed on it would go to the page body).
     */
    private Locator focusTarget() {
        return dramafinder instanceof FocusableElement focusable ? focusable.getFocusLocator() : locator;
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

    // ---------------------------------------------------------------- generic filters

    /** Filters for components without a Dramafinder element. */
    boolean matches(Filter f) {
        return switch (f.kind()) {
            case TEXT -> f.matches(getText());
            case LABEL -> f.matches(getProperty("label"));
            case PLACEHOLDER -> f.matches(getProperty("placeholder"));
            case ACCESSIBLE_NAME -> f.matches(accessibleName());
            case TEST_ID -> f.value().equals(getAttribute("data-testid"));
            case ID -> f.value().equals(getAttribute("id"));
            case CLASS_NAME -> Boolean.TRUE.equals(locator.evaluate("(e, c) => e.classList.contains(c)", f.value()));
            case THEME -> {
                String theme = getAttribute("theme");
                yield theme != null && List.of(theme.split("\\s+")).contains(f.value());
            }
            case ATTRIBUTE -> f.value().equals(getAttribute(f.name()));
        };
    }

    /** What a screen reader announces, approximated: aria-label, accessibleName, aria-labelledby target, label, text. */
    private String accessibleName() {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> names = (java.util.Map<String, Object>) locator.evaluate("""
                e => {
                  const by = e.getAttribute('aria-labelledby');
                  const target = by && document.getElementById(by);
                  return {aria: e.getAttribute('aria-label'), name: e.accessibleName,
                          by: target ? target.textContent : null, label: e.label};
                }""");
        for (String key : List.of("aria", "name", "by", "label")) {
            Object value = names.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return getText();
    }

    @Override
    public String toString() {
        return "PlaywrightElement[" + locator + "]";
    }
}
