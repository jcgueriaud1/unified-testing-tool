package com.vaadin.testing.unified.selenium;

import com.vaadin.flow.component.details.testbench.DetailsElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarButtonElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarItemElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarSubMenuElement;
import com.vaadin.flow.component.upload.testbench.UploadElement;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.DetailsAccess;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.openqa.selenium.WebElement;

/**
 * Access to Details, MenuBar and Upload on Selenium, through TestBench's element classes; a bare Tabs bar
 * uses {@link SeleniumTabSheetAccess}.
 */
final class SeleniumCoreSetAccess {
    private SeleniumCoreSetAccess() {}

    private static String text(WebElement element) {
        return element.getText().replaceAll("\\s+", " ").strip();
    }

    /** Through TestBench's DetailsElement. */
    record DetailsAccessImpl(SeleniumSession session, WebElement element) implements DetailsAccess {
        private DetailsElement details() {
            return session.testBench(element).wrap(DetailsElement.class);
        }

        @Override
        public String summaryText() {
            return details().getSummaryText().replaceAll("\\s+", " ").strip();
        }

        @Override
        public boolean isOpen() {
            return details().isOpened();
        }

        @Override
        public void toggle() {
            details().toggle();
            session.waitForVaadin();
        }

        @Override
        public UnifiedElement content() {
            return new SeleniumElement(session, details().getContent(), SeleniumComponents.DEFAULT);
        }
    }

    /**
     * Through TestBench's MenuBarElement: a top-level button opens its sub-menu when clicked, a nested item
     * when hovered (TestBench's {@code openSubMenu()}).
     */
    record MenuBarAccessImpl(SeleniumSession session, WebElement element) implements MenuBarAccess {
        private MenuBarElement menuBar() {
            return session.testBench(element).wrap(MenuBarElement.class);
        }

        @Override
        public List<String> items(List<String> path) {
            if (path.isEmpty()) {
                return menuBar().getButtons().stream().map(SeleniumCoreSetAccess::text).toList();
            }
            MenuBarSubMenuElement subMenu = open(path);
            try {
                return subMenu.getMenuItems().stream().map(SeleniumCoreSetAccess::text).toList();
            } finally {
                session.pressKey("Escape");
                if (menuBar().getAllSubMenus().stream().anyMatch(s -> s.hasAttribute("opened"))) {
                    session.pressKey("Escape");
                }
            }
        }

        @Override
        public void click(List<String> path) {
            if (path.size() == 1) {
                button(path.get(0)).click();
            } else {
                item(open(path.subList(0, path.size() - 1)), path, path.size() - 1).click();
            }
            session.waitForVaadin();
        }

        /** Opens the sub-menus along the path; returns the last one. */
        private MenuBarSubMenuElement open(List<String> path) {
            MenuBarSubMenuElement subMenu = button(path.get(0)).openSubMenu();
            for (int i = 1; i < path.size(); i++) {
                subMenu = item(subMenu, path, i).openSubMenu();
            }
            return subMenu;
        }

        private MenuBarButtonElement button(String text) {
            return menuBar().getButtons().stream().filter(b -> text.equals(text(b))).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No menu item \"" + text + "\" at [" + text + "]"));
        }

        private static MenuBarItemElement item(MenuBarSubMenuElement subMenu, List<String> path, int level) {
            String text = path.get(level);
            return subMenu.getMenuItems().stream().filter(i -> text.equals(text(i))).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No menu item \"" + text + "\" at "
                            + path.subList(0, level + 1)));
        }
    }

    /**
     * Through TestBench's UploadElement, which feeds the file input and waits until the upload finished.
     * WebDriver needs a file on disk: the content goes to a temporary file with the given name.
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
                session.testBench(element).wrap(UploadElement.class)
                        .upload(file.toFile(), (int) Math.max(1, session.timeout().toSeconds()));
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
