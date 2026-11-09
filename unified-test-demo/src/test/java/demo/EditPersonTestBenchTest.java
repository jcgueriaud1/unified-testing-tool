package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.grid.testbench.GridElement;
import com.vaadin.flow.component.grid.testbench.GridTHTDElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** TestBench (Selenium), without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class EditPersonTestBenchTest extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BrowserTest
    void editsAPersonFromTheGrid() {
        getDriver().get("http://localhost:" + port + "/people");
        GridElement people = $(GridElement.class).waitForFirst();

        GridTHTDElement alan = people.getCell("Alan Turing");
        int row = alan.getRow();
        alan.click();
        $(TextFieldElement.class).withLabel("Name").single().setValue("Alan M. Turing");
        $(ButtonElement.class).withText("Save").single().click();

        Assertions.assertEquals("Saved Alan M. Turing", $(NotificationElement.class).waitForFirst().getText());
        Assertions.assertEquals("Alan M. Turing", people.getCell(row, 0).getText());
    }
}
// --8<-- [end:test]
