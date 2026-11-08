package com.vaadin.testing.unified.spi;

import com.vaadin.testing.unified.UnifiedElement;
import java.util.List;

/** Adapter access to a TabSheet, reached by the core with {@code element.unwrap(TabSheetAccess.class)}. */
public interface TabSheetAccess {

    /** Labels of the tabs, in order. */
    List<String> tabs();

    /** Label of the selected tab, or null. */
    String selectedTab();

    /** Whether the user can click the tab with this label. */
    boolean isEnabled(String label);

    /** Clicks the tab with this label, as a user would. */
    void select(String label);

    /** The selected tab's content. */
    UnifiedElement content();
}
