package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.internal.tabs.TabSheetSupport;
import com.vaadin.testing.unified.query.Query;
import java.util.List;

/** A bare {@link Tabs} bar (no content panels: for those, {@link UnifiedTabSheet}). Tabs by their label. */
@UnifiedComponent(Tabs.class)
public interface UnifiedTabs extends UnifiedWrapper, Findable<UnifiedTabs.TabsQuery> {

    interface TabsQuery extends Query<UnifiedTabs, TabsQuery> {
        /** Tab bars having a tab with this label. A custom filter built from {@code where(..)}. */
        default TabsQuery withTab(String label) {
            return where(e -> e.unwrap(com.vaadin.testing.unified.spi.TabSheetAccess.class).tabs().contains(label));
        }
    }

    // actions (Auto-Wait); fail when the tab doesn't exist or isn't selected afterwards
    default void selectTab(String label) { TabSheetSupport.select(this, label); }

    // getters (immediate)
    default String getSelectedTab() { return TabSheetSupport.access(this).selectedTab(); }
    default List<String> getTabs() { return TabSheetSupport.access(this).tabs(); }
}
