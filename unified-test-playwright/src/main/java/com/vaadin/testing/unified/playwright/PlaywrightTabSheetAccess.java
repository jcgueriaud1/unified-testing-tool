package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.element.TabSheetElement;
import org.vaadin.addons.dramafinder.element.TabsElement;

/** TabSheet on Playwright through Dramafinder's TabSheetElement; tabs by their Item Name. */
final class PlaywrightTabSheetAccess implements TabSheetAccess {

    private final PlaywrightSession session;
    private final TabSheetElement tabSheet;

    PlaywrightTabSheetAccess(PlaywrightSession session, Locator locator) {
        this.session = session;
        this.tabSheet = new TabSheetElement(locator);
    }

    private TabsElement bar() {
        return tabSheet.getTabsElement();
    }

    @Override
    public List<String> tabs() {
        return bar().getTabLabels();
    }

    @Override
    public String selectedTab() {
        return PlaywrightCoreSetAccess.Tabs.selected(bar());
    }

    @Override
    public boolean isEnabled(String label) {
        return PlaywrightCoreSetAccess.Tabs.tab(bar(), label).isEnabled();
    }

    @Override
    public void select(String label) {
        PlaywrightCoreSetAccess.Tabs.select(session, bar(), label);
    }

    /** The selected tab's panel, not the panels of a TabSheet nested in it. */
    @Override
    public UnifiedElement content() {
        return new PlaywrightElement(session, tabSheet.getContentLocator(), null, PlaywrightComponents.DEFAULT);
    }
}
