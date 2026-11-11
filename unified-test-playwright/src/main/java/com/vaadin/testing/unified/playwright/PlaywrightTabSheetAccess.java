package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.element.TabSheetElement;

/** TabSheet on Playwright through Dramafinder's TabSheetElement; tabs by their Item Name. */
final class PlaywrightTabSheetAccess implements TabSheetAccess {

    private final PlaywrightSession session;
    private final TabSheetElement tabSheet;

    PlaywrightTabSheetAccess(PlaywrightSession session, Locator locator) {
        this.session = session;
        this.tabSheet = new TabSheetElement(locator);
    }

    private Locator tabLocators() {
        return tabSheet.getTabsElement().getTabs();
    }

    @Override
    public List<String> tabs() {
        return PlaywrightTabItems.names(tabLocators());
    }

    @Override
    public String selectedTab() {
        return PlaywrightTabItems.selected(tabLocators());
    }

    @Override
    public boolean isEnabled(String label) {
        return PlaywrightTabItems.isEnabled(tabLocators(), label);
    }

    @Override
    public void select(String label) {
        PlaywrightTabItems.select(session, tabLocators(), label);
    }

    /** The visible panel: Flow keeps the content of the other tabs out of the page or hidden. */
    @Override
    public UnifiedElement content() {
        Locator panel = tabSheet.getLocator().locator(":scope > [tab]:not([hidden])");
        return new PlaywrightElement(session, panel, null, PlaywrightComponents.DEFAULT);
    }
}
