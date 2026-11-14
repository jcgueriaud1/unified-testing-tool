package demo;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.VaadinElement;

/** A project's own Dramafinder element for the demo's Rating add-on: a custom element the migration leaves behind. */
public class RatingDramafinderElement extends VaadinElement {

    public RatingDramafinderElement(Locator locator) {
        super(locator);
    }

    public void rate(int stars) {
        getLocator().locator("span").nth(stars - 1).click();
        AutoWait.waitForVaadinIdle(getLocator().page());
    }

    public void assertValue(int stars) {
        PlaywrightAssertions.assertThat(getLocator()).hasJSProperty("value", stars);
    }
}
