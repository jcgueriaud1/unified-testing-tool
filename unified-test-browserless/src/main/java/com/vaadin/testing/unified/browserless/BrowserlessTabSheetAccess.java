package com.vaadin.testing.unified.browserless;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetTester;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;

/** TabSheet on Browserless through TabSheetTester, which works with tab labels. */
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

    @Override
    public List<String> tabs() {
        return tabSheet.getChildren().filter(Tabs.class::isInstance).flatMap(Component::getChildren)
                .filter(Tab.class::isInstance).map(t -> ((Tab) t).getLabel()).toList();
    }

    @Override
    public String selectedTab() {
        Tab selected = tabSheet.getSelectedTab();
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
        Component content = tabSheet.getComponent(tabSheet.getSelectedTab());
        return new BrowserlessElement(session, content);
    }
}
