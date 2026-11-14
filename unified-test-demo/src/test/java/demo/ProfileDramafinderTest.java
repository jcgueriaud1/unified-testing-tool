package demo;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

/**
 * Golden migration with raw Playwright calls and a custom element: unified-test-rewrite turns this into
 * ProfileDramafinderMigratedTest, an input of the skill evaluation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "playwright")
class ProfileDramafinderTest extends SpringPlaywrightIT {

    @Test
    void savesTheProfile() {
        navigate(ProfileView.class);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Name").setExact(true)).fill("Alice");
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Email").setExact(true)).fill("alice@example.com");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();

        NotificationElement.getByText(page, "Saved Alice").assertOpen();
    }

    @Test
    void ratesTheFood() {
        navigate(AddonView.class);
        RatingDramafinderElement food = new RatingDramafinderElement(page.locator("demo-rating").first());
        food.rate(3);

        food.assertValue(3);
    }
}
