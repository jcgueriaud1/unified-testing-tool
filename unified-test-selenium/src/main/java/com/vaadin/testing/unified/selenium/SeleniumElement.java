package com.vaadin.testing.unified.selenium;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.spi.Filter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Unified Element on Selenium: reads the DOM through JavaScript, acts through WebDriver or TestBench's own
 * element classes, then waits for Vaadin to finish the round-trip.
 */
final class SeleniumElement implements UnifiedElement {

    private final SeleniumSession session;
    private final WebElement element;
    private final SeleniumComponents.Support support;
    private final List<WebElement> roots;

    SeleniumElement(SeleniumSession session, WebElement element, SeleniumComponents.Support support) {
        this(session, element, support, List.of(element));
    }

    /** An element whose scope is several elements (e.g. a grid column's header cells). */
    SeleniumElement(SeleniumSession session, WebElement element, SeleniumComponents.Support support,
            List<WebElement> roots) {
        this.session = session;
        this.element = element;
        this.support = support;
        this.roots = roots;
    }

    WebElement webElement() {
        return element;
    }

    /** Where queries scoped to this element search: inside these elements. */
    List<? extends SearchContext> roots() {
        return roots;
    }

    private Object js(String script, Object... args) {
        Object[] all = new Object[args.length + 1];
        all[0] = element;
        System.arraycopy(args, 0, all, 1, args.length);
        return session.js(script, all);
    }

    // ---------------------------------------------------------------- reading

    @Override
    public String getTag() {
        return SeleniumSession.stale(element::getTagName);
    }

    /** The DOM text ({@code textContent}), whitespace collapsed: what Browserless reads on the server. */
    @Override
    public String getText() {
        Optional<String> special = SeleniumSession.stale(() -> support.text(element, session));
        if (special.isPresent()) {
            return BrowserQueries.normalize(special.get());
        }
        return (String) js("return (" + BrowserQueries.TEXT + ")(arguments[0]);");
    }

    @Override
    public String getVisibleText() {
        return SeleniumSession.stale(element::getText).strip();
    }

    @Override
    public String getInlineStyle(String name) {
        return (String) js("return (" + BrowserQueries.INLINE_STYLE + ")(arguments[0], arguments[1]);",
                BrowserQueries.cssName(name));
    }

    @Override
    public String getProperty(String name) {
        Object value = js("return arguments[0][arguments[1]];", name);
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
        Optional<Boolean> special = support.booleanProperty(element, name, session);
        if (special.isPresent()) {
            return special.get();
        }
        return Boolean.TRUE.equals(js("return !!arguments[0][arguments[1]];", name));
    }

    @Override
    public String getAttribute(String name) {
        return SeleniumSession.stale(() -> element.getDomAttribute(name));
    }

    @Override
    public String getCssValue(String property) {
        return (String) js("return getComputedStyle(arguments[0]).getPropertyValue(arguments[1]);", property);
    }

    @Override
    public boolean isVisible() {
        return SeleniumSession.stale(() -> support.visible(element, session).orElseGet(element::isDisplayed));
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(js("return !arguments[0].disabled && !arguments[0].hasAttribute('disabled');"));
    }

    @Override
    public List<UnifiedElement> findChildren(String tag) {
        return lightDom(BrowserQueries.lightDomXPath(tag, false));
    }

    @Override
    public List<UnifiedElement> findDescendants(String tag) {
        return lightDom(BrowserQueries.lightDomXPath(tag, true));
    }

    private List<UnifiedElement> lightDom(String xpath) {
        return SeleniumSession.stale(() -> element.findElements(By.xpath(xpath))).stream()
                .map(e -> (UnifiedElement) new SeleniumElement(session, e, SeleniumComponents.DEFAULT)).toList();
    }

    // ---------------------------------------------------------------- acting

    @Override
    public void setProperty(String name, Object value) {
        js("arguments[0][arguments[1]] = arguments[2];", name, value);
        session.waitForVaadin();
    }

    @Override
    public void click() {
        js("arguments[0].scrollIntoView({block: 'center', inline: 'center'});");
        try {
            SeleniumSession.stale(element::click);
        } catch (ElementNotInteractableException e) { // incl. ElementClickInterceptedException
            js("arguments[0].click();"); // e.g. a host element without a box of its own
        }
        session.waitForVaadin();
    }

    /** Double-clicks, then waits for Vaadin. */
    void doubleClick() {
        js("arguments[0].scrollIntoView({block: 'center', inline: 'center'});");
        new Actions(session.driver()).doubleClick(element).perform();
        session.waitForVaadin();
    }

    @Override
    public void setValueAsUser(Object value) {
        if (!support.setValue(element, value, session)) {
            String text = value == null ? ""
                    : value instanceof Number n ? new BigDecimal(n.toString()).stripTrailingZeros().toPlainString()
                    : String.valueOf(value);
            // what TestBench's setValue does: set the value, then the events a user's input fires
            js("""
                    const e = arguments[0];
                    e.value = arguments[1];
                    e.dispatchEvent(new Event('input', {bubbles: true, composed: true}));
                    e.dispatchEvent(new Event('change', {bubbles: true, composed: true}));""", text);
        }
        session.waitForVaadin();
    }

    @Override
    public void focus() {
        js("const t = arguments[0].focusElement || arguments[0].inputElement || arguments[0]; t.focus();");
        session.waitForVaadin();
    }

    @Override
    public void blur() {
        js("const t = arguments[0].focusElement || arguments[0].inputElement || arguments[0]; t.blur();");
        session.waitForVaadin();
    }

    /**
     * Keys go to the part that takes focus (a field's input, not its host). Elements that can't be focused
     * (a dialog host) get the keys at page level, where e.g. Escape closes the open overlay.
     */
    @Override
    public void pressKey(String key) {
        WebElement target = (WebElement) js("return arguments[0].focusElement || arguments[0].inputElement || arguments[0];");
        try {
            SeleniumSession.stale(() -> target.sendKeys(SeleniumKeys.chord(key)));
        } catch (ElementNotInteractableException e) {
            new Actions(session.driver()).sendKeys(SeleniumKeys.chord(key)).perform();
        }
        session.waitForVaadin();
    }

    @Override
    public void dispatchEvent(String eventName) {
        js("arguments[0].dispatchEvent(new CustomEvent(arguments[1], {bubbles: true, composed: true}));", eventName);
        session.waitForVaadin();
    }

    // ---------------------------------------------------------------- Escape Hatch

    @Override
    public <T> T unwrap(Class<T> type) {
        return tryUnwrap(type).orElseThrow(() -> new UnsupportedOperationException(
                "Selenium can't provide " + type.getName() + " for " + this));
    }

    @Override
    public <T> Optional<T> tryUnwrap(Class<T> type) {
        Optional<Object> special = support.unwrap(type, element, session);
        if (special.isPresent()) {
            return special.map(type::cast);
        }
        if (TestBenchElement.class.isAssignableFrom(type)) {
            // TestBench's element class for this component, e.g. TextFieldElement
            @SuppressWarnings("unchecked")
            Class<? extends TestBenchElement> tb = (Class<? extends TestBenchElement>) type;
            return Optional.of(type.cast(session.testBench(element).wrap(tb)));
        }
        if (type.isInstance(element)) {
            return Optional.of(type.cast(element));
        }
        if (WebDriver.class.isAssignableFrom(type) && type.isInstance(session.driver())) {
            return Optional.of(type.cast(session.driver()));
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- filters

    boolean matches(Filter f) {
        return switch (f.kind()) {
            case TEXT -> f.matches(getText());
            case LABEL -> f.matches(getProperty("label"));
            case PLACEHOLDER -> f.matches(getProperty("placeholder"));
            case ACCESSIBLE_NAME -> f.matches(accessibleName());
            case TEST_ID -> f.value().equals(getAttribute("data-testid"));
            case ID -> f.value().equals(getAttribute("id"));
            case CLASS_NAME -> Boolean.TRUE.equals(js("return arguments[0].classList.contains(arguments[1]);", f.value()));
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
        Map<String, Object> names = (Map<String, Object>) js("""
                const e = arguments[0];
                const by = e.getAttribute('aria-labelledby');
                const target = by && document.getElementById(by);
                return {aria: e.getAttribute('aria-label'), name: e.accessibleName,
                        by: target ? target.textContent : null, label: e.label};""");
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
        return "SeleniumElement[" + element + "]";
    }
}
