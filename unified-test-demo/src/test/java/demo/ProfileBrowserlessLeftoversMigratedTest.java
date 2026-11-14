package demo;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration with leftovers: unified-test-rewrite turns this into ProfileBrowserlessLeftoversMigratedTest. */
@UnifiedSpringBootTest
class ProfileBrowserlessLeftoversMigratedTest {

    @Test
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void savesTheProfile(UnifiedPage page) {
        BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
        // TODO unified: Browserless Escape Hatch: navigate(ProfileView.class)
        ProfileView view = browserless.navigate(ProfileView.class);
        TextField name = (TextField) view.getChildren().findFirst().orElseThrow();
        // TODO unified: Component Reference: test(name).setValue("Alice"): replace it with a locator that finds the same component
        browserless.test(name).setValue("Alice");
        // TODO unified: filter runs Java on the component: test($(TextField.class).withCondition(TextField::isRequiredIndicatorVisible).single()).setValue("alice@example.com"): replace withCondition(..) with a locator
        browserless.test(browserless.$(TextField.class).withCondition(TextField::isRequiredIndicatorVisible).single())
                .setValue("alice@example.com");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alice");
    }
}
