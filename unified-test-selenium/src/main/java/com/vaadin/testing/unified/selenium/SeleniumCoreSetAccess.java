package com.vaadin.testing.unified.selenium;

import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.DetailsAccess;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Access to Details, MenuBar and Upload on Selenium; a bare Tabs bar uses {@link SeleniumTabSheetAccess}. */
final class SeleniumCoreSetAccess {
    private SeleniumCoreSetAccess() {}

    record DetailsAccessImpl(SeleniumSession session, WebElement element) implements DetailsAccess {
        private static final String SUMMARY = "arguments[0].querySelector(':scope > vaadin-details-summary')";

        @Override
        public String summaryText() {
            return (String) session.js("const s = " + SUMMARY + "; return s ? s.textContent.trim() : null;", element);
        }

        @Override
        public boolean isOpen() {
            return Boolean.TRUE.equals(session.js("return !!arguments[0].opened;", element));
        }

        @Override
        public void toggle() {
            WebElement summary = (WebElement) session.js("return " + SUMMARY + ";", element);
            new SeleniumElement(session, summary, SeleniumComponents.DEFAULT).click();
        }

        /** Everything but the summary: Flow puts the content in a div. */
        @Override
        public UnifiedElement content() {
            List<WebElement> content = element.findElements(By.xpath("./*[not(self::vaadin-details-summary)]"));
            return new SeleniumElement(session, element, SeleniumComponents.DEFAULT, content);
        }
    }

    /**
     * Top-level items are the menu bar's buttons; a sub-menu is the last visible list box in the menu bar
     * (each opened sub-menu comes after its parent in the page). The same as the Playwright adapter.
     */
    record MenuBarAccessImpl(SeleniumSession session, WebElement element) implements MenuBarAccess {
        private static final String HELPERS = """
                const bar = arguments[0];
                const text = e => e.textContent.replace(/\\s+/g, ' ').trim();
                const visible = e => !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
                const topLevel = () => [...bar.querySelectorAll(':scope > vaadin-menu-bar-button')]
                    .filter(b => !b.hasAttribute('hidden'));
                const openMenus = () => [...bar.querySelectorAll('vaadin-menu-bar-list-box')].filter(visible);
                const items = level => level === 0 ? topLevel()
                    : [...openMenus().pop().querySelectorAll(':scope > vaadin-menu-bar-item')];
                """;

        private long openCount() {
            return (Long) session.js(HELPERS + "return openMenus().length;", element);
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<String> items(List<String> path) {
            if (path.isEmpty()) {
                return (List<String>) session.js(HELPERS + "return topLevel().map(text);", element);
            }
            open(path);
            try {
                return (List<String>) session.js(HELPERS + "return items(1).map(text);", element);
            } finally {
                session.pressKey("Escape");
                if (openCount() > 0) {
                    session.pressKey("Escape");
                }
            }
        }

        @Override
        public void click(List<String> path) {
            if (path.size() > 1) {
                open(path.subList(0, path.size() - 1));
            }
            click(item(path, path.size() - 1));
        }

        /**
         * A DOM click on the item: WebDriver's native click on a menu-bar button doesn't open its sub-menu in
         * headless Chrome, a DOM click does (as the menu bar's own keyboard handling does).
         */
        private void click(WebElement item) {
            session.js("arguments[0].click();", item);
            session.waitForVaadin();
        }

        private void open(List<String> path) {
            for (int i = 0; i < path.size(); i++) {
                long before = openCount();
                WebElement item = item(path, i);
                if (i == 0) {
                    click(item);
                } else {
                    // a nested item opens its sub-menu on hover; a DOM click doesn't open it
                    session.js("arguments[0].dispatchEvent(new MouseEvent('mouseover', {bubbles: true, composed: true}));",
                            item);
                    session.waitForVaadin();
                }
                new WebDriverWait(session.driver(), Duration.ofSeconds(5)).until(d -> openCount() > before);
            }
        }

        private WebElement item(List<String> path, int level) {
            WebElement item = (WebElement) session.js(HELPERS
                    + "return items(arguments[1]).find(i => text(i) === arguments[2]) || null;",
                    element, level, path.get(level));
            if (item == null) {
                throw new IllegalArgumentException("No menu item \"" + path.get(level) + "\" at "
                        + path.subList(0, level + 1));
            }
            return item;
        }
    }

    /**
     * Sends the file to the upload's file input, in its shadow root, then waits until every file has finished
     * uploading. WebDriver needs a file on disk: the content goes to a temporary file with the given name.
     */
    record UploadAccessImpl(SeleniumSession session, WebElement element) implements UploadAccess {
        @Override
        public void upload(String fileName, String mimeType, byte[] content) {
            Path file;
            try {
                file = Files.createTempDirectory("unified-upload").resolve(fileName);
                Files.write(file, content);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            try {
                // through JavaScript: TestBench's element proxy doesn't support getShadowRoot()
                WebElement input = (WebElement) session.js(
                        "return arguments[0].shadowRoot.querySelector('input[type=file]');", element);
                input.sendKeys(file.toAbsolutePath().toString());
                new WebDriverWait(session.driver(), session.timeout()).until(d -> Boolean.TRUE.equals(session.js(
                        "const e = arguments[0]; return (e.files || []).length > 0"
                                + " && e.files.every(f => f.complete || f.error || f.abort);", element)));
                session.waitForVaadin();
            } finally {
                try {
                    Files.deleteIfExists(file);
                    Files.deleteIfExists(file.getParent());
                } catch (IOException ignored) {
                    // a temporary file
                }
            }
        }
    }
}
