package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.FilePayload;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.DetailsAccess;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.DetailsElement;
import org.vaadin.addons.dramafinder.element.TabElement;
import org.vaadin.addons.dramafinder.element.TabsElement;

/** Access to Tabs, Details, MenuBar and Upload on Playwright. */
final class PlaywrightCoreSetAccess {
    private PlaywrightCoreSetAccess() {}

    /** A bare Tabs bar through Dramafinder's TabsElement: TabSheetAccess without content. */
    record TabsAccess(PlaywrightSession session, Locator locator) implements TabSheetAccess {
        private TabsElement bar() {
            return new TabsElement(locator);
        }

        @Override
        public List<String> tabs() {
            return bar().getTabs().all().stream().map(t -> new TabElement(t).getLabel().strip()).toList();
        }

        @Override
        public String selectedTab() {
            Locator selected = bar().getTabs().and(locator.page().locator("[selected]"));
            return selected.count() == 0 ? null : new TabElement(selected.first()).getLabel().strip();
        }

        @Override
        public boolean isEnabled(String label) {
            return !bar().getTab(label).getLocator().first().isDisabled();
        }

        @Override
        public void select(String label) {
            bar().selectTab(label);
            AutoWait.waitForVaadinIdle(session.page());
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
        private static final String TEXTS = "els => els.map(e => e.textContent.replace(/\\s+/g, ' ').trim())";

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
                return (List<String>) topLevel().evaluateAll(TEXTS);
            }
            open(path);
            try {
                return (List<String>) openMenu().locator("xpath=./vaadin-menu-bar-item").evaluateAll(TEXTS);
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

        private Locator item(List<String> path, int level) {
            Locator candidates = level == 0 ? topLevel() : openMenu().locator("xpath=./vaadin-menu-bar-item");
            String text = path.get(level);
            Locator item = candidates.filter(new Locator.FilterOptions().setHasText(
                    PlaywrightSession.exactText(text)));
            if (item.count() == 0) {
                throw new IllegalArgumentException("No menu item \"" + text + "\" at " + path.subList(0, level + 1));
            }
            return item.first();
        }

        private void close() {
            locator.page().keyboard().press("Escape");
            if (locator.locator("vaadin-menu-bar-list-box:visible").count() > 0) {
                locator.page().keyboard().press("Escape");
            }
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
