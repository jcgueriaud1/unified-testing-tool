package demo;

import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.UnifiedUITest;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration of a narrow Spring context: unified-test-rewrite turns this into ProfileBrowserlessNarrowContextMigratedTest. */
// TODO unified: Browserless-only pattern: a Spring context without @SpringBootTest has no web server; use @UnifiedSpringBootTest with @MockitoBean to run everywhere (spec 02)
@ContextConfiguration(classes = ProfileService.class)
@ExtendWith(SpringExtension.class)
@RequiresTestingTool(TestingTool.BROWSERLESS)
@UnifiedUITest
class ProfileBrowserlessNarrowContextMigratedTest {

    @Test
    void savesTheProfile(UnifiedPage page) {
        page.navigate(ProfileView.class);
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");
        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("alice@example.com");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alice");
    }
}
