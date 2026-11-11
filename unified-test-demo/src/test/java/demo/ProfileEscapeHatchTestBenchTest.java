package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Golden migration with leftovers: unified-test-rewrite turns this into ProfileEscapeHatchMigratedTest. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class ProfileEscapeHatchTestBenchTest extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BeforeEach
    void open() {
        getDriver().get("http://localhost:" + port + "/profile");
    }

    @BrowserTest
    void savesTheName() {
        TextFieldElement name = $(TextFieldElement.class).withLabel("Name").first();
        name.setValue("Alice");
        save();

        Assertions.assertEquals("Alice", name.getValue());
    }

    @BrowserTest
    void knowsTheArchiveButtonIsDisabled() {
        Assertions.assertTrue($(ButtonElement.class).withText("Archive").single().hasAttribute("disabled"));
        Assertions.assertEquals("profile", executeScript("return document.location.pathname.substring(1)"));
    }

    private void save() {
        $(ButtonElement.class).withText("Save").single().click();
    }
}
