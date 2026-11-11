package com.vaadin.testing.unified.browserless;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetTester;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;

/** TabSheet on Browserless through TabSheetTester; tabs are found by their Item Name. */
final class BrowserlessTabSheetAccess implements TabSheetAccess {

    private final BrowserlessSession session;
    private final TabSheet tabSheet;

    BrowserlessTabSheetAccess(BrowserlessSession session, TabSheet tabSheet) {
        this.session = session;
        this.tabSheet = tabSheet;
    }

    private TabSheetTester<?> tester() {
        return (TabSheetTester<?>) session.window().test(tabSheet);
    }

    private List<Tab> all() {
        return tabSheet.getChildren().filter(Tabs.class::isInstance).flatMap(Component::getChildren)
                .filter(Tab.class::isInstance).map(Tab.class::cast).toList();
    }

    @Override
    public List<String> tabs() {
        return all().stream().map(ItemNames::of).toList();
    }

    @Override
    public String selectedTab() {
        Tab selected = tabSheet.getSelectedTab();
        return selected == null ? null : ItemNames.of(selected);
    }

    @Override
    public boolean isEnabled(String label) {
        return ItemNames.find(all(), label, "tab \"" + label + "\"").isEnabled();
    }

    /** By position: TabSheetTester.select(label) compares the label, which a tab holding a component lacks. */
    @Override
    public void select(String label) {
        session.act(() -> {
            List<Tab> all = all();
            tester().select(ItemNames.visiblePosition(all, ItemNames.find(all, label, "tab \"" + label + "\""), "Tab"));
            session.window().roundTrip();
        });
    }

    @Override
    public UnifiedElement content() {
        Component content = tabSheet.getComponent(tabSheet.getSelectedTab());
        return new BrowserlessElement(session, content);
    }
}
