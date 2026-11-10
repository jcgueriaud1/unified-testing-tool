package com.vaadin.testing.unified;

import java.util.List;

/**
 * Portable, low-level handle to one component. Every Testing Tool supports these operations:
 * Browserless on the server-side element and component, browser Testing Tools on the DOM.
 * On Browserless a property is the server-side value; it equals the browser's only for properties Flow
 * synchronises.
 * <p>
 * Actions wait until the component is visible and enabled, then for the server round-trip (Auto-Wait).
 */
public interface UnifiedElement extends HasEscapeHatch {
    String getTag();

    /**
     * The element's text as in the DOM ({@code textContent}), whitespace collapsed and trimmed. The same on
     * every Testing Tool: CSS doesn't change it ({@code text-transform}, hidden descendants, line breaks
     * between flex items). For the text as rendered, see {@link #getVisibleText()}.
     */
    String getText();

    /**
     * The text as the browser renders it ({@code innerText}): CSS applied, hidden parts left out, line breaks
     * between blocks. Needs a real browser ({@code @RequiresBrowser}).
     */
    String getVisibleText();

    String getProperty(String name);
    Integer getPropertyInteger(String name);
    Boolean getPropertyBoolean(String name);
    void setProperty(String name, Object value);

    String getAttribute(String name);

    /** Computed CSS value. Needs a real browser ({@code @RequiresBrowser}). */
    String getCssValue(String property);

    /**
     * The value the element's inline style declares for a property, as written ({@code var(--gap)}, not the
     * computed value), or null. The same on every Testing Tool. {@code name} is dash-separated or camelCase.
     */
    String getInlineStyle(String name);

    boolean isVisible();
    boolean isEnabled();

    // user actions, with Auto-Wait
    void click();
    void setValueAsUser(Object value);
    void focus();
    void blur();
    /** Simulated on Browserless. */
    void pressKey(String key);
    void dispatchEvent(String eventName);

    /**
     * The element's direct children with this tag ({@code *}: any), in order. Light DOM only: what the app put
     * there, never a web component's shadow DOM. Text nodes don't count. The same on every Testing Tool.
     */
    List<UnifiedElement> findChildren(String tag);

    /** The element's direct children, in order: {@code findChildren("*")}. */
    default List<UnifiedElement> children() {
        return findChildren("*");
    }

    /**
     * Every element below this one with this tag ({@code *}: any), in document order. Light DOM only, as
     * {@link #findChildren}. Note that web components add light DOM of their own in a browser (a text field's
     * input and label), which Browserless doesn't have.
     */
    List<UnifiedElement> findDescendants(String tag);
}
