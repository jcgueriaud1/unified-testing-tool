package demo;

import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.testbench.BrowserTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Golden migration of a TestBench base type, a test extending it and a page object (includePageObjects). */
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class ProfileSubclassTestBenchTest extends AbstractProfileTestBench {

    @BrowserTest
    void savesTheProfile() {
        ProfileTestBenchPage profile = new ProfileTestBenchPage(this);
        profile.fill("Alice", "alice@example.com");
        profile.saveButton().click();

        Assertions.assertEquals("Saved Alice", $(NotificationElement.class).waitForFirst().getText());
    }
}
