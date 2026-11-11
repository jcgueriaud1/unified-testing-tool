package com.vaadin.testing.unified.browserless;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsTester;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsTester;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadTester;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.DetailsAccess;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.util.List;

/** Access to Tabs, Details, MenuBar and Upload on Browserless, through their testers. */
final class BrowserlessCoreSetAccess {
    private BrowserlessCoreSetAccess() {}

    /** A bare Tabs bar: TabSheetAccess without content. Tabs are found by their Item Name. */
    record TabsAccess(BrowserlessSession session, Tabs bar) implements TabSheetAccess {
        private List<Tab> all() {
            return bar.getChildren().filter(Tab.class::isInstance).map(Tab.class::cast).toList();
        }

        @Override
        public List<String> tabs() {
            return all().stream().map(ItemNames::of).toList();
        }

        @Override
        public String selectedTab() {
            Tab selected = bar.getSelectedTab();
            return selected == null ? null : ItemNames.of(selected);
        }

        @Override
        public boolean isEnabled(String label) {
            return ItemNames.find(all(), label, "tab \"" + label + "\"").isEnabled();
        }

        /** By position: TabsTester.select(label) compares the label, which a tab holding a component lacks. */
        @Override
        public void select(String label) {
            session.act(() -> {
                List<Tab> all = all();
                int position = ItemNames.visiblePosition(all, ItemNames.find(all, label, "tab \"" + label + "\""), "Tab");
                ((TabsTester<?>) session.window().test(bar)).select(position);
                session.window().roundTrip();
            });
        }

        @Override
        public UnifiedElement content() {
            throw new UnsupportedOperationException("A Tabs bar has no content; use UnifiedTabSheet for a TabSheet");
        }
    }

    record DetailsAccessImpl(BrowserlessSession session, Details details) implements DetailsAccess {
        @Override
        public String summaryText() {
            return details.getSummaryText();
        }

        @Override
        public boolean isOpen() {
            return details.isOpened();
        }

        @Override
        public void toggle() {
            session.act(() -> {
                ((DetailsTester<?>) session.window().test(details)).toggleDetails();
                session.window().roundTrip();
            });
        }

        @Override
        public UnifiedElement content() {
            List<Component> content = details.getContent().toList();
            return new BrowserlessElement(session, details, content);
        }
    }

    /**
     * Items found by their Item Name, then clicked by position: MenuBarTester matches an item's own text,
     * which an icon-only item lacks.
     */
    record MenuBarAccessImpl(BrowserlessSession session, MenuBar menuBar) implements MenuBarAccess {

        @Override
        public List<String> items(List<String> path) {
            List<MenuItem> items = path.isEmpty() ? menuBar.getItems()
                    : resolve(path).get(path.size() - 1).getSubMenu().getItems();
            return items.stream().filter(Component::isVisible).map(ItemNames::of).toList();
        }

        @Override
        public void click(List<String> path) {
            session.act(() -> {
                List<MenuItem> chosen = resolve(path);
                int[] positions = new int[path.size()];
                List<MenuItem> level = menuBar.getItems();
                for (int i = 0; i < path.size(); i++) {
                    positions[i] = ItemNames.visiblePosition(level, chosen.get(i), "Menu item");
                    level = chosen.get(i).getSubMenu().getItems();
                }
                ((MenuBarTester<?>) session.window().test(menuBar)).clickItem(positions[0],
                        java.util.Arrays.copyOfRange(positions, 1, positions.length));
                session.window().roundTrip();
            });
        }

        @Override
        public boolean isEnabled(List<String> path) {
            return resolve(path).get(path.size() - 1).isEnabled();
        }

        /** The item of each level of the path. */
        private List<MenuItem> resolve(List<String> path) {
            List<MenuItem> chosen = new java.util.ArrayList<>();
            List<MenuItem> level = menuBar.getItems();
            for (int i = 0; i < path.size(); i++) {
                MenuItem item = ItemNames.find(level, path.get(i), "menu item \"" + path.get(i) + "\" at " + path.subList(0, i + 1));
                chosen.add(item);
                level = item.getSubMenu().getItems();
            }
            return chosen;
        }
    }

    record UploadAccessImpl(BrowserlessSession session, Upload upload) implements UploadAccess {
        @Override
        public void upload(String fileName, String mimeType, byte[] content) {
            session.act(() -> {
                ((UploadTester<?>) session.window().test(upload)).upload(fileName, mimeType, content);
                session.window().roundTrip();
            });
        }
    }
}
