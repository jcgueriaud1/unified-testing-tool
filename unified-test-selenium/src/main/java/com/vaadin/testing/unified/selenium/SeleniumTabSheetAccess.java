package com.vaadin.testing.unified.selenium;

import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** TabSheet on Selenium: tabs by their text, the content is the panel that isn't hidden. */
final class SeleniumTabSheetAccess implements TabSheetAccess {

    private static final String TABS = "[...arguments[0].querySelectorAll('vaadin-tab')]";

    private final SeleniumSession session;
    private final WebElement element;

    SeleniumTabSheetAccess(SeleniumSession session, WebElement element) {
        this.session = session;
        this.element = element;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> tabs() {
        return (List<String>) session.js("return " + TABS + ".map(t => t.textContent.trim());", element);
    }

    @Override
    public String selectedTab() {
        return (String) session.js("const t = " + TABS + ".find(t => t.hasAttribute('selected'));"
                + " return t ? t.textContent.trim() : null;", element);
    }

    @Override
    public boolean isEnabled(String label) {
        return Boolean.TRUE.equals(session.js("const t = " + TABS + ".find(t => t.textContent.trim() === arguments[1]);"
                + " return !!t && !t.hasAttribute('disabled');", element, label));
    }

    @Override
    public void select(String label) {
        WebElement tab = (WebElement) session.js("return " + TABS + ".find(t => t.textContent.trim() === arguments[1]);",
                element, label);
        new SeleniumElement(session, tab, SeleniumComponents.DEFAULT).click();
    }

    /** The visible panel: Flow keeps the content of the other tabs out of the page or hidden. */
    @Override
    public UnifiedElement content() {
        WebElement panel = element.findElement(By.cssSelector(":scope > [tab]:not([hidden])"));
        return new SeleniumElement(session, panel, SeleniumComponents.DEFAULT);
    }
}
