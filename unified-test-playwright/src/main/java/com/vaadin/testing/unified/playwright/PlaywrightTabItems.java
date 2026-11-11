package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.testing.unified.internal.BrowserQueries;
import java.util.List;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.TabElement;

/**
 * The tabs of a Tabs bar or a TabSheet, by their Item Name ({@link BrowserQueries#ITEM_NAME}): Dramafinder
 * finds a tab by its accessible name, which an {@code aria-label} overrides even when the tab has a text.
 */
final class PlaywrightTabItems {
    private PlaywrightTabItems() {}

    @SuppressWarnings("unchecked")
    static List<String> names(Locator tabs) {
        return (List<String>) tabs.evaluateAll("els => els.map(" + BrowserQueries.ITEM_NAME + ")");
    }

    static String selected(Locator tabs) {
        return (String) tabs.evaluateAll("els => { const t = els.find(e => e.hasAttribute('selected'));"
                + " return t ? (" + BrowserQueries.ITEM_NAME + ")(t) : null; }");
    }

    static boolean isEnabled(Locator tabs, String name) {
        return !tab(tabs, name).isDisabled();
    }

    static void select(PlaywrightSession session, Locator tabs, String name) {
        new TabElement(tab(tabs, name)).select();
        AutoWait.waitForVaadinIdle(session.page());
    }

    private static Locator tab(Locator tabs, String name) {
        List<String> names = names(tabs);
        int index = names.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("No tab \"" + name + "\"; there are " + names);
        }
        return tabs.nth(index);
    }
}
