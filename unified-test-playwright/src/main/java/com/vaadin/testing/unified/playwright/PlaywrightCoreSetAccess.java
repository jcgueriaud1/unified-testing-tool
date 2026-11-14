package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.FilePayload;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.DateTimePickerAccess;
import com.vaadin.testing.unified.spi.DetailsAccess;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.DateTimePickerElement;
import org.vaadin.addons.dramafinder.element.DetailsElement;
import org.vaadin.addons.dramafinder.element.MenuBarElement;
import org.vaadin.addons.dramafinder.element.MenuElement;
import org.vaadin.addons.dramafinder.element.MenuItemElement;
import org.vaadin.addons.dramafinder.element.TabElement;
import org.vaadin.addons.dramafinder.element.TabsElement;
import org.vaadin.addons.dramafinder.element.UploadElement;

/**
 * Access to Tabs, Details, MenuBar, Upload and DateTimePicker on Playwright, through Dramafinder's elements.
 * Dramafinder's element actions wait for the server round-trip themselves; only actions on raw locators
 * wait here.
 */
final class PlaywrightCoreSetAccess {
    private PlaywrightCoreSetAccess() {}

    /** Fails at once, listing the names, when no item has this one: a locator matching nothing would wait. */
    static void requireName(List<String> names, String name, String what) {
        if (!names.contains(name)) {
            throw new IllegalArgumentException("No " + what + " \"" + name + "\"; there are " + names);
        }
    }

    /**
     * Tabs of a Tabs bar or a TabSheet by their Item Name: Dramafinder's tab label is the tab's own text, or
     * its {@code aria-label} when it shows no text.
     */
    static final class Tabs {
        private Tabs() {}

        static String selected(TabsElement bar) {
            int index = bar.getSelectedIndex();
            return index < 0 ? null : bar.getTab(index).getLabel();
        }

        static TabElement tab(TabsElement bar, String label) {
            requireName(bar.getTabLabels(), label, "tab");
            return bar.getTab(label);
        }

        static void select(PlaywrightSession session, TabsElement bar, String label) {
            tab(bar, label).select();
            AutoWait.waitForVaadinIdle(session.page());
        }
    }

    /** A bare Tabs bar through Dramafinder's TabsElement: TabSheetAccess without content. */
    record TabsAccess(PlaywrightSession session, Locator locator) implements TabSheetAccess {
        private TabsElement bar() {
            return new TabsElement(locator);
        }

        @Override
        public List<String> tabs() {
            return bar().getTabLabels();
        }

        @Override
        public String selectedTab() {
            return Tabs.selected(bar());
        }

        @Override
        public boolean isEnabled(String label) {
            return Tabs.tab(bar(), label).isEnabled();
        }

        @Override
        public void select(String label) {
            Tabs.select(session, bar(), label);
        }

        @Override
        public UnifiedElement content() {
            throw new UnsupportedOperationException("A Tabs bar has no content; use UnifiedTabSheet for a TabSheet");
        }
    }

    record DetailsAccessImpl(PlaywrightSession session, Locator locator) implements DetailsAccess {
        private DetailsElement details() {
            return new DetailsElement(locator);
        }

        @Override
        public String summaryText() {
            return details().getSummaryText().strip();
        }

        @Override
        public boolean isOpen() {
            return details().isOpen();
        }

        @Override
        public void toggle() {
            details().toggle(); // waits for the round-trip
        }

        /** Everything but the summary, open or closed. */
        @Override
        public UnifiedElement content() {
            return new PlaywrightElement(session, details().getContentLocator(), null, PlaywrightComponents.DEFAULT);
        }
    }

    /** Through Dramafinder's MenuBarElement: items by their Item Name, each sub-menu scoped to its level. */
    record MenuBarAccessImpl(PlaywrightSession session, Locator locator) implements MenuBarAccess {
        private MenuBarElement bar() {
            return new MenuBarElement(locator);
        }

        @Override
        public List<String> items(List<String> path) {
            if (path.isEmpty()) {
                return bar().getItemLabels();
            }
            MenuElement menu = open(path);
            try {
                return menu.getItemLabels();
            } finally {
                bar().close();
            }
        }

        @Override
        public void click(List<String> path) {
            if (path.size() == 1) {
                topLevel(path.get(0)).click();
            } else {
                item(open(path.subList(0, path.size() - 1)), path).click();
            } // Dramafinder's click waits for the round-trip
        }

        @Override
        public boolean isEnabled(List<String> path) {
            if (path.size() == 1) {
                return topLevel(path.get(0)).isEnabled();
            }
            MenuElement menu = open(path.subList(0, path.size() - 1));
            try {
                return item(menu, path).isEnabled();
            } finally {
                bar().close();
            }
        }

        /** Opens the menus of every item of the path; returns the last one opened. */
        private MenuElement open(List<String> path) {
            topLevel(path.get(0));
            MenuElement menu = bar().openSubMenu(path.get(0));
            for (int i = 1; i < path.size(); i++) {
                item(menu, path.subList(0, i + 1));
                menu = menu.openSubMenu(path.get(i));
            }
            return menu;
        }

        private MenuItemElement topLevel(String name) {
            requireName(bar().getItemLabels(), name, "menu item");
            return bar().getMenuItemElement(name);
        }

        /** The last item of {@code path}, in {@code menu}. */
        private static MenuItemElement item(MenuElement menu, List<String> path) {
            String name = path.get(path.size() - 1);
            requireName(menu.getItemLabels(), name, "menu item at " + path);
            return menu.getMenuItemElement(name);
        }
    }

    /** Types into both halves, each committed with Enter, then leaves the field, as a user does. */
    record DateTimePickerAccessImpl(PlaywrightSession session, Locator locator) implements DateTimePickerAccess {
        @Override
        public void typeText(String date, String time) {
            new DateTimePickerElement(locator).typeText(date, time); // waits for the round-trip
        }
    }

    /** Returns once the upload request has finished and the round trip after it is done. */
    record UploadAccessImpl(PlaywrightSession session, Locator locator) implements UploadAccess {
        @Override
        public void upload(String fileName, String mimeType, byte[] content) {
            new UploadElement(locator).uploadFiles(new FilePayload(fileName, mimeType, content));
            AutoWait.waitForVaadinIdle(session.page());
        }
    }
}
