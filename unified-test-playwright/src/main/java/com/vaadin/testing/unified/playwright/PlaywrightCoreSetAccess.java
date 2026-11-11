package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.FilePayload;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.spi.DateTimePickerAccess;
import com.vaadin.testing.unified.spi.DetailsAccess;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.DetailsElement;
import org.vaadin.addons.dramafinder.element.TabsElement;

/** Access to Tabs, Details, MenuBar, Upload and DateTimePicker on Playwright. */
final class PlaywrightCoreSetAccess {
    private PlaywrightCoreSetAccess() {}

    /** A bare Tabs bar through Dramafinder's TabsElement: TabSheetAccess without content; tabs by Item Name. */
    record TabsAccess(PlaywrightSession session, Locator locator) implements TabSheetAccess {
        private TabsElement bar() {
            return new TabsElement(locator);
        }

        @Override
        public List<String> tabs() {
            return PlaywrightTabItems.names(bar().getTabs());
        }

        @Override
        public String selectedTab() {
            return PlaywrightTabItems.selected(bar().getTabs());
        }

        @Override
        public boolean isEnabled(String label) {
            return PlaywrightTabItems.isEnabled(bar().getTabs(), label);
        }

        @Override
        public void select(String label) {
            PlaywrightTabItems.select(session, bar().getTabs(), label);
        }

        @Override
        public UnifiedElement content() {
            throw new UnsupportedOperationException("A Tabs bar has no content; use UnifiedTabSheet for a TabSheet");
        }
    }

    record DetailsAccessImpl(PlaywrightSession session, Locator locator) implements DetailsAccess {
        @Override
        public String summaryText() {
            return new DetailsElement(locator).getSummaryText().strip();
        }

        @Override
        public boolean isOpen() {
            return new DetailsElement(locator).isOpen();
        }

        @Override
        public void toggle() {
            new DetailsElement(locator).getSummaryLocator().click();
            AutoWait.afterAction(session.page());
        }

        /** Everything but the summary: Flow puts the content in a div. */
        @Override
        public UnifiedElement content() {
            Locator content = locator.locator("xpath=./*[not(self::vaadin-details-summary)]");
            return new PlaywrightElement(session, content, null, PlaywrightComponents.DEFAULT);
        }
    }

    /**
     * Top-level items are the menu bar's buttons; a sub-menu is the last visible list box in the menu bar
     * (each opened sub-menu comes after its parent in the page).
     */
    record MenuBarAccessImpl(PlaywrightSession session, Locator locator) implements MenuBarAccess {
        private static final String NAMES = "els => els.map(" + BrowserQueries.ITEM_NAME + ")";
        private static final String ENABLED = "e => !(e.hasAttribute('disabled') || e.getAttribute('aria-disabled') === 'true')";

        private Locator topLevel() {
            return locator.locator("xpath=./vaadin-menu-bar-button[not(@hidden)]");
        }

        private Locator openMenus() {
            return locator.locator("vaadin-menu-bar-list-box:visible");
        }

        /** The sub-menu opened last, pinned: opening the next one doesn't change what it points to. */
        private Locator openMenu() {
            int open = openMenus().count();
            if (open == 0) {
                throw new IllegalStateException("No sub-menu is open");
            }
            return openMenus().nth(open - 1);
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<String> items(List<String> path) {
            if (path.isEmpty()) {
                return (List<String>) topLevel().evaluateAll(NAMES);
            }
            open(path);
            try {
                return (List<String>) openMenu().locator("xpath=./vaadin-menu-bar-item").evaluateAll(NAMES);
            } finally {
                close();
            }
        }

        @Override
        public void click(List<String> path) {
            if (path.size() > 1) {
                open(path.subList(0, path.size() - 1));
            }
            item(path, path.size() - 1).click();
            AutoWait.afterAction(session.page());
        }

        @Override
        public boolean isEnabled(List<String> path) {
            if (path.size() == 1) {
                return Boolean.TRUE.equals(item(path, 0).evaluate(ENABLED));
            }
            open(path.subList(0, path.size() - 1));
            try {
                return Boolean.TRUE.equals(item(path, path.size() - 1).evaluate(ENABLED));
            } finally {
                close();
            }
        }

        /** Opens the menus of every item of the path: each click opens one more sub-menu. */
        private void open(List<String> path) {
            for (int i = 0; i < path.size(); i++) {
                int before = openMenus().count();
                item(path, i).click();
                AutoWait.afterAction(session.page());
                locator.page().waitForCondition(() -> openMenus().count() > before,
                        new Page.WaitForConditionOptions().setTimeout(5000));
            }
        }

        /** The item of this Item Name at this level of the path. */
        @SuppressWarnings("unchecked")
        private Locator item(List<String> path, int level) {
            Locator candidates = level == 0 ? topLevel() : openMenu().locator("xpath=./vaadin-menu-bar-item");
            String name = path.get(level);
            List<String> names = (List<String>) candidates.evaluateAll(NAMES);
            int index = names.indexOf(name);
            if (index < 0) {
                throw new IllegalArgumentException("No menu item \"" + name + "\" at " + path.subList(0, level + 1)
                        + "; there are " + names);
            }
            return candidates.nth(index);
        }

        /**
         * Closes the open menus and waits until they are gone: a closing sub-menu stays visible while it
         * animates, and opening the menu again would count it as open.
         */
        private void close() {
            locator.page().keyboard().press("Escape");
            if (locator.locator("vaadin-menu-bar-submenu[opened]:not([closing])").count() > 0) {
                locator.page().keyboard().press("Escape");
            }
            locator.page().waitForCondition(() -> openMenus().count() == 0,
                    new Page.WaitForConditionOptions().setTimeout(5000));
        }
    }

    /**
     * Types into the inputs of the date and time halves, each committed with Enter, then leaves the field as
     * a user does: the field checks its halves (bad input, incomplete) when focus leaves it.
     */
    record DateTimePickerAccessImpl(PlaywrightSession session, Locator locator) implements DateTimePickerAccess {
        @Override
        public void typeText(String date, String time) {
            type(locator.locator("[slot=date-picker] input"), date);
            Locator timeInput = locator.locator("[slot=time-picker] input");
            type(timeInput, time);
            timeInput.blur();
            AutoWait.afterAction(session.page());
        }

        private static void type(Locator input, String text) {
            input.fill(text);
            input.press("Enter");
        }
    }

    /** Feeds the upload's file input, then waits until every file has finished uploading. */
    record UploadAccessImpl(PlaywrightSession session, Locator locator) implements UploadAccess {
        @Override
        public void upload(String fileName, String mimeType, byte[] content) {
            locator.locator("input[type=\"file\"]").first().setInputFiles(new FilePayload(fileName, mimeType, content));
            locator.page().waitForCondition(() -> Boolean.TRUE.equals(locator.evaluate(
                    "e => (e.files || []).length > 0 && e.files.every(f => f.complete || f.error || f.abort)")));
            AutoWait.waitForVaadinIdle(session.page());
        }
    }
}
