package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Golden migration: unified-test-rewrite turns ProfileTestBenchTest into ProfileMigratedTest. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class ProfileTestBenchTest extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BrowserTest
    void savesTheProfile() {
        getDriver().get("http://localhost:" + port + "/profile");

        $(TextFieldElement.class).withLabel("Name").single().setValue("Alice");
        $(TextFieldElement.class).withLabel("Email").single().setValue("alice@example.com");
        $(ButtonElement.class).withText("Save").single().click();

        Assertions.assertEquals("Alice", $(TextFieldElement.class).withLabel("Name").single().getValue());
        Assertions.assertEquals("Archive", $(ButtonElement.class).withText("Archive").single().getText());
    }
}
