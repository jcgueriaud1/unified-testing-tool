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
    String getText();

    String getProperty(String name);
    Integer getPropertyInteger(String name);
    Boolean getPropertyBoolean(String name);
    void setProperty(String name, Object value);

    String getAttribute(String name);

    /** Computed CSS value. Needs a real browser ({@code @RequiresBrowser}). */
    String getCssValue(String property);

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

    List<UnifiedElement> findChildren(String tag);
}
