package com.vaadin.testing.unified.selenium;

import com.vaadin.flow.component.datetimepicker.testbench.DateTimePickerElement;
import com.vaadin.flow.component.details.testbench.DetailsElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarButtonElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarItemElement;
import com.vaadin.flow.component.menubar.testbench.MenuBarSubMenuElement;
import com.vaadin.flow.component.timepicker.testbench.TimePickerElement;
import com.vaadin.flow.component.upload.testbench.UploadElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.spi.DateTimePickerAccess;
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
 * Access to Details, MenuBar, Upload and DateTimePicker on Selenium, through TestBench's element classes; a bare Tabs bar
 * uses {@link SeleniumTabSheetAccess}.
 */
final class SeleniumCoreSetAccess {
    private SeleniumCoreSetAccess() {}

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
     * when hovered (TestBench's {@code openSubMenu()}). Items are found by their Item Name.
     */
    record MenuBarAccessImpl(SeleniumSession session, WebElement element) implements MenuBarAccess {
        private MenuBarElement menuBar() {
            return session.testBench(element).wrap(MenuBarElement.class);
        }

        @Override
        public List<String> items(List<String> path) {
            if (path.isEmpty()) {
                return names(menuBar().getButtons());
            }
            MenuBarSubMenuElement subMenu = open(path);
            try {
                return names(subMenu.getMenuItems());
            } finally {
                close();
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

        @Override
        public boolean isEnabled(List<String> path) {
            if (path.size() == 1) {
                return enabled(button(path.get(0)));
            }
            try {
                return enabled(item(open(path.subList(0, path.size() - 1)), path, path.size() - 1));
            } finally {
                close();
            }
        }

        /** A disabled item keeps focus with aria-disabled, which TestBench's isEnabled() misses: #162. */
        private static boolean enabled(TestBenchElement item) {
            return !item.hasAttribute("disabled") && !"true".equals(item.getDomAttribute("aria-disabled"));
        }

        private void close() {
            session.pressKey("Escape");
            if (menuBar().getAllSubMenus().stream().anyMatch(s -> s.hasAttribute("opened"))) {
                session.pressKey("Escape");
            }
        }

        /** The Item Names of these items, in one round trip. */
        @SuppressWarnings("unchecked")
        private List<String> names(List<? extends WebElement> items) {
            return (List<String>) session.js("return arguments[0].map(" + BrowserQueries.ITEM_NAME + ");", items); // #161
        }

        /** Opens the sub-menus along the path; returns the last one. */
        private MenuBarSubMenuElement open(List<String> path) {
            MenuBarSubMenuElement subMenu = button(path.get(0)).openSubMenu();
            for (int i = 1; i < path.size(); i++) {
                subMenu = item(subMenu, path, i).openSubMenu();
            }
            return subMenu;
        }

        private MenuBarButtonElement button(String name) {
            return named(menuBar().getButtons(), List.of(name), 0);
        }

        private MenuBarItemElement item(MenuBarSubMenuElement subMenu, List<String> path, int level) {
            return named(subMenu.getMenuItems(), path, level);
        }

        private <T extends WebElement> T named(List<T> items, List<String> path, int level) {
            List<String> names = names(items);
            int index = names.indexOf(path.get(level));
            if (index < 0) {
                throw new IllegalArgumentException("No menu item \"" + path.get(level) + "\" at "
                        + path.subList(0, level + 1) + "; there are " + names);
            }
            return items.get(index);
        }
    }

    /**
     * Types into the date and time halves through TestBench's DateTimePickerElement, each committed with
     * Enter, then leaves the field as a user does: the field checks its halves (bad input, incomplete) when
     * focus leaves it.
     */
    record DateTimePickerAccessImpl(SeleniumSession session, WebElement element) implements DateTimePickerAccess {
        @Override
        public void typeText(String date, String time) {
            DateTimePickerElement picker = session.testBench(element).wrap(DateTimePickerElement.class);
            picker.getDatePicker().setInputValue(date);
            TimePickerElement timePicker = picker.getTimePicker();
            timePicker.selectByText(time);
            timePicker.getTimePickerInputElement().callFunction("blur"); // no TestBenchElement.blur(): #164
            session.waitForVaadin();
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
