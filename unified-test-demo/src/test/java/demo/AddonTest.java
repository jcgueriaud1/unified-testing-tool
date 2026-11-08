package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGenericComponent;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import demo.addon.Rating;
import demo.addon.testing.UnifiedBadge;
import demo.addon.testing.UnifiedBadgeBrowserless;
import demo.addon.testing.UnifiedRating;
import org.junit.jupiter.api.Test;

/** Add-on wrappers (spec 06, ADR 0003); written once, runs on Browserless and Playwright. */
@UnifiedSpringBootTest
class AddonTest {

    @Test
    void wrapperWithoutOverride(UnifiedPage page) {
        page.navigate(AddonView.class);
        UnifiedRating food = page.find(UnifiedRating.class).withLabel("Food").single();

        food.rate(3);

        page.waitUntil(() -> food.getValue() == 3);
        assertEquals(4, page.find(UnifiedRating.class).withLabel("Service").single().getValue());
    }

    @Test
    void customFilters(UnifiedPage page) {
        page.navigate(AddonView.class);

        // custom filter from where(..)
        assertEquals(1, page.find(UnifiedRating.class).withValue(4).all().size());
        // custom filters can also be built inline from withProperty
        assertEquals(1, page.find(UnifiedRating.class).withProperty("label", "Service").all().size());
        assertEquals(2, page.find(UnifiedRating.class).all().size());
    }

    @Test
    void wrapperWithToolOverride(UnifiedPage page) {
        page.navigate(AddonView.class);
        UnifiedBadge inbox = page.find(UnifiedBadge.class).withTestId("inbox").single();
        page.waitUntil(() -> inbox.getCount() == 2);

        page.find(UnifiedButton.class).withText("Receive").single().click();

        page.waitUntil(() -> inbox.getCount() == 3);
        // the override is what runs on Browserless; elsewhere the wrapper's own default methods
        boolean browserless = page.unwrap(Object.class).getClass().getName().contains("browserless");
        assertEquals(browserless, inbox instanceof UnifiedBadgeBrowserless);
    }

    @Test
    void anyComponentWithoutAWrapper(UnifiedPage page) {
        page.navigate(AddonView.class);

        UnifiedGenericComponent service = page.findComponent(Rating.class).withLabel("Service").single();

        assertEquals(4, service.element().getPropertyInteger("value"));
        assertTrue(service.isVisible());
    }
}
