package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.ScreenshotOnFailureRule;
import com.vaadin.testbench.TestBenchTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

/** Golden migration of a JUnit 4 TestBench test: compile-only here, as the demo runs no JUnit 4 tests. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JUnit4TestBenchTest extends TestBenchTestCase {

    @Rule
    public ScreenshotOnFailureRule screenshots = new ScreenshotOnFailureRule(this, true);

    @LocalServerPort
    int port;

    @Before
    public void openProfile() {
        setDriver(new ChromeDriver());
        getDriver().get("http://localhost:" + port + "/profile");
    }

    @Test
    public void savesTheName() {
        $(TextFieldElement.class).withLabel("Name").single().setValue("Alice");
        $(TextFieldElement.class).withLabel("Email").single().setValue("alice@example.com");
        $(ButtonElement.class).withText("Save").single().click();

        Assert.assertEquals("Saved Alice", $(NotificationElement.class).waitForFirst().getText());
    }

    @After
    public void tearDown() {
        getDriver().quit();
    }
}
