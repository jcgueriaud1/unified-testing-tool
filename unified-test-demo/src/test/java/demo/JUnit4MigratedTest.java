package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration of a JUnit 4 TestBench test: compile-only here, as the demo runs no JUnit 4 tests. */
@UnifiedSpringBootTest
public class JUnit4MigratedTest {

    public void openProfile(UnifiedPage page) {
        page.navigate(ProfileView.class);
    }

    @Test
    public void savesTheName(UnifiedPage page) {
        openProfile(page);
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");
        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("alice@example.com");
        page.find(UnifiedButton.class).withText("Save").single().click();

        // TODO unified: was waitForFirst(); single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alice");
    }
}
