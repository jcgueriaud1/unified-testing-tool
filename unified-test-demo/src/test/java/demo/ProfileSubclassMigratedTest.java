package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedNotification;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration of a TestBench base type, a test extending it and a page object (includePageObjects). */
class ProfileSubclassMigratedTest extends AbstractProfileMigrated {

    @Test
    void savesTheProfile(UnifiedPage page) {
        openProfile(page);
        ProfileMigratedPage profile = new ProfileMigratedPage(page);
        profile.fill("Alice", "alice@example.com");
        profile.saveButton().click();

        // TODO unified: was waitForFirst(); single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alice");
    }
}
