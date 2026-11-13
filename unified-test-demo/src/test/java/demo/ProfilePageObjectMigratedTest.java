package demo;

import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration of a page object user without includePageObjects; its twin is an input of the skill evaluation. */
@UnifiedSpringBootTest
class ProfilePageObjectMigratedTest {

    @Test
    @RequiresTestingTool(TestingTool.SELENIUM)
    void savesTheProfile(UnifiedPage page) {
        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
        page.navigate(ProfileView.class);

        // TODO unified: TestBench page object: new ProfileTestBenchPage(this)
        ProfileTestBenchPage profile = new ProfileTestBenchPage(testBench);
        profile.fill("Bob", "bob@example.com");
        profile.saveButton().click();

        // TODO unified: was waitForFirst(); single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Bob");
    }
}
