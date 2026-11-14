package com.vaadin.testing.unified.selenium;

import com.vaadin.flow.component.tabs.testbench.TabElement;
import com.vaadin.flow.component.tabs.testbench.TabSheetElement;
import com.vaadin.flow.component.tabs.testbench.TabsElement;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * TabSheet and a bare Tabs bar on Selenium, through TestBench's TabSheetElement and TabsElement. Tabs are
 * found by their Item Name, which TestBench's tab label doesn't match (#161). The content is the sheet's own
 * panel that isn't hidden (#160).
 */
final class SeleniumTabSheetAccess implements TabSheetAccess {

    private final SeleniumSession session;
    private final WebElement element;
    private final boolean sheet;

    private SeleniumTabSheetAccess(SeleniumSession session, WebElement element, boolean sheet) {
        this.session = session;
        this.element = element;
        this.sheet = sheet;
    }

    /** A TabSheet ({@code vaadin-tabsheet}). */
    static SeleniumTabSheetAccess tabSheet(SeleniumSession session, WebElement element) {
        return new SeleniumTabSheetAccess(session, element, true);
    }

    /** A bare Tabs bar ({@code vaadin-tabs}), without content. */
    static SeleniumTabSheetAccess tabs(SeleniumSession session, WebElement element) {
        return new SeleniumTabSheetAccess(session, element, false);
    }

    private TabsElement tabsElement() {
        return sheet ? session.testBench(element).wrap(TabSheetElement.class).getTabs()
                : session.testBench(element).wrap(TabsElement.class);
    }

    @Override
    public List<String> tabs() {
        return names(tabsElement().getTabs());
    }

    @Override
    public String selectedTab() {
        TabsElement tabs = tabsElement();
        Integer selected = tabs.getPropertyInteger("selected");
        return selected == null || selected < 0 ? null : names(List.of(tabs.getSelectedTabElement())).get(0);
    }

    @Override
    public boolean isEnabled(String label) {
        return tab(label).isEnabled();
    }

    @Override
    public void select(String label) {
        new SeleniumElement(session, tab(label), SeleniumComponents.DEFAULT).click();
    }

    /** The tab of this Item Name. */
    private TabElement tab(String label) {
        List<TabElement> tabs = tabsElement().getTabs();
        List<String> names = names(tabs);
        int index = names.indexOf(label);
        if (index < 0) {
            throw new IllegalArgumentException("No tab \"" + label + "\"; there are " + names);
        }
        return tabs.get(index);
    }

    /** The Item Names of these tabs, in one round trip (#161). */
    @SuppressWarnings("unchecked")
    private List<String> names(List<? extends WebElement> tabs) {
        return (List<String>) session.js("return arguments[0].map(" + BrowserQueries.ITEM_NAME + ");", tabs);
    }

    /** The visible panel: Flow keeps the content of the other tabs out of the page or hidden. */
    @Override
    public UnifiedElement content() {
        if (!sheet) {
            throw new UnsupportedOperationException("A Tabs bar has no content; use a TabSheet");
        }
        // TabSheetElement.getContent() can return a nested sheet's panel: #160
        WebElement panel = element.findElement(By.cssSelector(":scope > [tab]:not([hidden])"));
        return new SeleniumElement(session, panel, SeleniumComponents.DEFAULT);
    }
}
