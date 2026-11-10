package com.vaadin.testing.unified.browserless;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsTester;
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

    /** A bare Tabs bar: TabSheetAccess without content. */
    record TabsAccess(BrowserlessSession session, Tabs bar) implements TabSheetAccess {
        private TabsTester<?> tester() {
            return (TabsTester<?>) session.window().test(bar);
        }

        @Override
        public List<String> tabs() {
            return bar.getChildren().filter(Tab.class::isInstance).map(t -> ((Tab) t).getLabel()).toList();
        }

        @Override
        public String selectedTab() {
            Tab selected = bar.getSelectedTab();
            return selected == null ? null : selected.getLabel();
        }

        @Override
        public boolean isEnabled(String label) {
            return tester().getTab(label).isEnabled();
        }

        @Override
        public void select(String label) {
            session.act(() -> {
                tester().select(label);
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

    record MenuBarAccessImpl(BrowserlessSession session, MenuBar menuBar) implements MenuBarAccess {
        private MenuBarTester<?> tester() {
            return (MenuBarTester<?>) session.window().test(menuBar);
        }

        @Override
        public List<String> items(List<String> path) {
            if (path.isEmpty()) {
                return tester().getItemTexts();
            }
            return tester().getItemTexts(path.get(0), path.subList(1, path.size()).toArray(String[]::new));
        }

        @Override
        public void click(List<String> path) {
            session.act(() -> {
                tester().clickItem(path.get(0), path.subList(1, path.size()).toArray(String[]::new));
                session.window().roundTrip();
            });
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
