package demo;

import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Golden migration of a page object user without includePageObjects; its twin is an input of the skill evaluation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class ProfilePageObjectTestBenchTest extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BrowserTest
    void savesTheProfile() {
        getDriver().get("http://localhost:" + port + "/profile");

        ProfileTestBenchPage profile = new ProfileTestBenchPage(this);
        profile.fill("Bob", "bob@example.com");
        profile.saveButton().click();

        Assertions.assertEquals("Saved Bob", $(NotificationElement.class).waitForFirst().getText());
    }
}
