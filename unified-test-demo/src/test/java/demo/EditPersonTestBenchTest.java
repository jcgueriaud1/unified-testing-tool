package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.grid.testbench.GridElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchTestCase;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** TestBench (Selenium), without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class EditPersonTestBenchTest extends TestBenchTestCase {

    @LocalServerPort
    int port;

    @BeforeEach
    void openBrowser() {
        setDriver(TestBench.createDriver(new ChromeDriver(new ChromeOptions().addArguments("--headless=new"))));
    }

    @AfterEach
    void closeBrowser() {
        getDriver().quit();
    }

    @Test
    void editsAPersonFromTheGrid() {
        getDriver().get("http://localhost:" + port + "/people");
        GridElement people = $(GridElement.class).waitForFirst();

        int row = IntStream.range(0, people.getRowCount())
                .filter(i -> people.getCell(i, 0).getText().equals("Alan Turing"))
                .findFirst().orElseThrow();
        people.getCell(row, 0).click();
        $(TextFieldElement.class).withLabel("Name").single().setValue("Alan M. Turing");
        $(ButtonElement.class).withText("Save").single().click();

        assertEquals("Saved Alan M. Turing", $(NotificationElement.class).waitForFirst().getText());
        assertEquals("Alan M. Turing", people.getCell(row, 0).getText());
    }
}
// --8<-- [end:test]
