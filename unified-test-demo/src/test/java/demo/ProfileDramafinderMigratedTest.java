package demo;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/**
 * Golden migration with raw Playwright calls and a custom element: unified-test-rewrite turns this into
 * ProfileDramafinderMigratedTest, an input of the skill evaluation.
 */
@UnifiedSpringBootTest
class ProfileDramafinderMigratedTest {

    @Test
    @RequiresTestingTool(TestingTool.PLAYWRIGHT)
    void savesTheProfile(UnifiedPage page) {
        Page playwright = page.unwrap(Page.class);
        page.navigate(ProfileView.class);
        // TODO unified: Dramafinder Escape Hatch: playwright.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Name").setExact(true)).fill("Alice")
        playwright.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Name").setExact(true)).fill("Alice");
        // TODO unified: Dramafinder Escape Hatch: playwright.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Email").setExact(true)).fill("alice@example.com")
        playwright.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Email").setExact(true)).fill("alice@example.com");
        // TODO unified: Dramafinder Escape Hatch: playwright.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click()
        playwright.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();

        // TODO unified: was getByText(..), a substring and first match; single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).withTextContaining("Saved Alice").single()).isOpen();
    }

    @Test
    @RequiresTestingTool(TestingTool.PLAYWRIGHT)
    void ratesTheFood(UnifiedPage page) {
        Page playwright = page.unwrap(Page.class);
        page.navigate(AddonView.class);
        // TODO unified: Dramafinder Escape Hatch: new RatingDramafinderElement(playwright.locator("demo-rating").first()), a custom element: write a Wrapper for it (ADR 0003)
        RatingDramafinderElement food = new RatingDramafinderElement(playwright.locator("demo-rating").first());
        // TODO unified: Dramafinder Escape Hatch: food.rate(3)
        food.rate(3);

        // TODO unified: Dramafinder Escape Hatch: food.assertValue(3)
        food.assertValue(3);
    }
}
