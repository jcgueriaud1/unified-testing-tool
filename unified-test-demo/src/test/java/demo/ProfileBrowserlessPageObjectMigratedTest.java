package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration of a test and its page object, with includePageObjects (GoldenMigrationTest). */
@UnifiedSpringBootTest
class ProfileBrowserlessPageObjectMigratedTest {

    @Test
    void savesTheProfile(UnifiedPage page) {
        ProfileBrowserlessMigratedPage profile = new ProfileBrowserlessMigratedPage(page);
        profile.open();
        profile.save("Alice", "alice@example.com");

        assertThat(profile.notification()).hasText("Saved Alice");
    }
}
