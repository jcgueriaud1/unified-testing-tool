package com.vaadin.testing.unified.selenium;

import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** TabSheet on Selenium: tabs by their Item Name, the content is the panel that isn't hidden. */
final class SeleniumTabSheetAccess implements TabSheetAccess {

    private static final String TABS = "[...arguments[0].querySelectorAll('vaadin-tab')]";
    private static final String NAME = "const name = " + BrowserQueries.ITEM_NAME + ";";

    private final SeleniumSession session;
    private final WebElement element;

    SeleniumTabSheetAccess(SeleniumSession session, WebElement element) {
        this.session = session;
        this.element = element;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> tabs() {
        return (List<String>) session.js(NAME + " return " + TABS + ".map(name);", element);
    }

    @Override
    public String selectedTab() {
        return (String) session.js(NAME + " const t = " + TABS + ".find(t => t.hasAttribute('selected'));"
                + " return t ? name(t) : null;", element);
    }

    @Override
    public boolean isEnabled(String label) {
        return tab(label).getDomAttribute("disabled") == null;
    }

    @Override
    public void select(String label) {
        new SeleniumElement(session, tab(label), SeleniumComponents.DEFAULT).click();
    }

    /** The tab of this Item Name. */
    private WebElement tab(String label) {
        WebElement tab = (WebElement) session.js(NAME + " return " + TABS + ".find(t => name(t) === arguments[1]);",
                element, label);
        if (tab == null) {
            throw new IllegalArgumentException("No tab \"" + label + "\"; there are " + tabs());
        }
        return tab;
    }

    /** The visible panel: Flow keeps the content of the other tabs out of the page or hidden. */
    @Override
    public UnifiedElement content() {
        WebElement panel = element.findElement(By.cssSelector(":scope > [tab]:not([hidden])"));
        return new SeleniumElement(session, panel, SeleniumComponents.DEFAULT);
    }
}
