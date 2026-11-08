package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.TabElement;
import org.vaadin.addons.dramafinder.element.TabSheetElement;

/** TabSheet on Playwright through Dramafinder's TabSheetElement. */
final class PlaywrightTabSheetAccess implements TabSheetAccess {

    private final PlaywrightSession session;
    private final TabSheetElement tabSheet;

    PlaywrightTabSheetAccess(PlaywrightSession session, Locator locator) {
        this.session = session;
        this.tabSheet = new TabSheetElement(locator);
    }

    @Override
    public List<String> tabs() {
        return tabSheet.getTabsElement().getTabs().all().stream().map(t -> new TabElement(t).getLabel().strip())
                .toList();
    }

    @Override
    public String selectedTab() {
        Locator selected = tabSheet.getTabsElement().getTabs().and(tabSheet.getLocator().page().locator("[selected]"));
        return selected.count() == 0 ? null : new TabElement(selected.first()).getLabel().strip();
    }

    @Override
    public boolean isEnabled(String label) {
        return !tabSheet.getTab(label).getLocator().first().isDisabled();
    }

    @Override
    public void select(String label) {
        tabSheet.selectTab(label);
        AutoWait.waitForVaadinIdle(session.page());
    }

    /** The visible panel: Flow keeps the content of the other tabs out of the page or hidden. */
    @Override
    public UnifiedElement content() {
        Locator panel = tabSheet.getLocator().locator(":scope > [tab]:not([hidden])");
        return new PlaywrightElement(session, panel, null, PlaywrightComponents.DEFAULT);
    }
}
