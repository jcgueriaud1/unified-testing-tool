package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration: unified-test-rewrite turns ProfileTestBenchTest into ProfileMigratedTest. */
@UnifiedSpringBootTest
class ProfileMigratedTest {

    @Test
    void savesTheProfile(UnifiedPage page) {
        page.navigate(ProfileView.class);

        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");
        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("alice@example.com");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedTextField.class).withLabel("Name").single()).hasValue("Alice");
        assertThat(page.find(UnifiedButton.class).withText("Archive").single()).hasText("Archive");
    }
}
