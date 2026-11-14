package com.vaadin.testing.unified.selenium;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
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
 * Unified Element on Selenium: reads and acts through TestBench's element API ({@link TestBenchElement})
 * and WebDriver, then waits for Vaadin to finish the round-trip.
 * <p>
 * The scripts left here have no TestBench API yet; each names the issue that would replace it.
 */
final class SeleniumElement implements UnifiedElement {

    private final SeleniumSession session;
    private final WebElement element;
    private final SeleniumComponents.Support support;
    /** Null until needed: the support's content element (e.g. a notification's card). */
    private List<WebElement> roots;

    SeleniumElement(SeleniumSession session, WebElement element, SeleniumComponents.Support support) {
        this(session, element, support, null);
    }

    /** An element whose scope is several elements (e.g. a grid column's header cells); null = its content. */
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
        if (roots == null) {
            roots = List.of(SeleniumSession.stale(() -> support.content(element, session)));
        }
        return roots;
    }

    private Object js(String script, Object... args) {
        Object[] all = new Object[args.length + 1];
        all[0] = element;
        System.arraycopy(args, 0, all, 1, args.length);
        return session.js(script, all);
    }

    /** The element as TestBench's element, whose API the reads and actions below use. */
    private TestBenchElement tb() {
        return session.testBench(element);
    }

    /** A property read through TestBench, stale elements turned into the core's retryable exception. */
    private Object property(String name) {
        return SeleniumSession.stale(() -> tb().getProperty(name));
    }

    /** JavaScript truthiness of a value read from the browser. */
    private static boolean truthy(Object value) {
        if (value == null || value instanceof Boolean b && !b) {
            return false;
        }
        if (value instanceof Number n) {
            double d = n.doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        return !(value instanceof String s && s.isEmpty());
    }

    /**
     * The part that takes focus: the component's focus element or input (Vaadin fields), else the element
     * itself.
     */
    private TestBenchElement focusTarget() {
        for (String name : List.of("focusElement", "inputElement")) {
            if (property(name) instanceof TestBenchElement target) {
                return target;
            }
        }
        return tb();
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
        return BrowserQueries.normalize((String) property("textContent"));
    }

    @Override
    public String getVisibleText() {
        WebElement content = (WebElement) roots().get(0);
        return SeleniumSession.stale(content::getText).strip();
    }

    @Override
    public String getInlineStyle(String name) {
        // no TestBench API reads an inline style: #168
        return (String) js("return (" + BrowserQueries.INLINE_STYLE + ")(arguments[0], arguments[1]);",
                BrowserQueries.cssName(name));
    }

    @Override
    public String getProperty(String name) {
        Object value = property(name);
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
        return truthy(property(name));
    }

    @Override
    public String getAttribute(String name) {
        return SeleniumSession.stale(() -> element.getDomAttribute(name));
    }

    @Override
    public String getCssValue(String property) {
        // WebDriver's getCssValue rewrites colors to rgba(..), unlike the other Testing Tools: #169
        return (String) js("return getComputedStyle(arguments[0]).getPropertyValue(arguments[1]);", property);
    }

    @Override
    public boolean isVisible() {
        return SeleniumSession.stale(() -> support.visible(element, session).orElseGet(element::isDisplayed));
    }

    @Override
    public boolean isEnabled() {
        return !truthy(property("disabled")) && !SeleniumSession.stale(() -> tb().hasAttribute("disabled"));
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
        TestBenchElement tb = tb();
        SeleniumSession.stale(() -> {
            switch (value) {
                case null -> tb.setProperty(name, (String) null);
                case String text -> tb.setProperty(name, text);
                case Boolean b -> tb.setProperty(name, b);
                case Integer i -> tb.setProperty(name, i);
                case Short i -> tb.setProperty(name, (int) i);
                case Byte i -> tb.setProperty(name, (int) i);
                case Number n -> tb.setProperty(name, n.doubleValue());
                default -> throw new IllegalArgumentException("TestBench sets String, Boolean and number"
                        + " properties, not " + value.getClass().getName());
            }
        });
        session.waitForVaadin();
    }

    @Override
    public void click() {
        scrollToCenter();
        try {
            SeleniumSession.stale(element::click);
        } catch (ElementNotInteractableException e) { // incl. ElementClickInterceptedException
            SeleniumSession.stale(() -> tb().callFunction("click")); // e.g. a host element without a box of its own
        }
        session.waitForVaadin();
    }

    /** Double-clicks, then waits for Vaadin. */
    void doubleClick() {
        scrollToCenter();
        new Actions(session.driver()).doubleClick(element).perform();
        session.waitForVaadin();
    }

    private void scrollToCenter() {
        SeleniumSession.stale(() -> tb().scrollIntoView(Map.of("block", "center", "inline", "center")));
    }

    @Override
    public void setValueAsUser(Object value) {
        if (!support.setValue(element, value, session)) {
            String text = BrowserQueries.typedText(value);
            // what TestBench's setValue does: set the value, then the events a user's input fires
            TestBenchElement tb = tb();
            SeleniumSession.stale(() -> {
                tb.setProperty("value", text);
                tb.dispatchEvent("input", Map.of("bubbles", true, "composed", true));
                tb.dispatchEvent("change", Map.of("bubbles", true, "composed", true));
            });
        }
        session.waitForVaadin();
    }

    @Override
    public void focus() {
        SeleniumSession.stale(() -> focusTarget().callFunction("focus"));
        session.waitForVaadin();
    }

    @Override
    public void blur() {
        SeleniumSession.stale(() -> focusTarget().callFunction("blur")); // no TestBenchElement.blur(): #164
        session.waitForVaadin();
    }

    /**
     * Keys go to the part that takes focus (a field's input, not its host). Elements that can't be focused
     * (a dialog host) get the keys at page level, where e.g. Escape closes the open overlay.
     */
    @Override
    public void pressKey(String key) {
        WebElement target = focusTarget();
        try {
            SeleniumSession.stale(() -> target.sendKeys(SeleniumKeys.chord(key)));
        } catch (ElementNotInteractableException e) {
            new Actions(session.driver()).sendKeys(SeleniumKeys.chord(key)).perform();
        }
        session.waitForVaadin();
    }

    @Override
    public void dispatchEvent(String eventName) {
        SeleniumSession.stale(() -> tb().dispatchEvent(eventName, Map.of("bubbles", true, "composed", true)));
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

    @Override
    public String toString() {
        return "SeleniumElement[" + element + "]";
    }
}
