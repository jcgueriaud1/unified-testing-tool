package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.grid.GridLocator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/** Browserless Test, without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@SpringBootTest
class EditPersonBrowserlessTest {

    @TestConfiguration
    static class BrowserlessConfiguration {
        @Bean
        BrowserlessApplicationContext browserless(ApplicationContext spring) {
            // the app uses Spring Security: users of a secured context get their own credentials
            return SpringBrowserlessApplicationContext.createSecured(spring, PeopleView.class);
        }
    }

    @Autowired
    BrowserlessApplicationContext app;

    BrowserlessUIContext window;

    @BeforeEach
    void openWindow() {
        window = app.newUser().newWindow();
    }

    @AfterEach
    void closeUser() {
        window.getUser().close();
    }

    @Test
    void editsAPersonFromTheGrid() {
        window.navigate(PeopleView.class);
        GridLocator<PeopleView.Person> people = window.findGrid(PeopleView.Person.class);

        int row = IntStream.range(0, people.size())
                .filter(i -> people.getCellText(i, 0).equals("Alan Turing"))
                .findFirst().orElseThrow();
        people.clickRow(row);
        window.findTextField().withLabel("Name").setValue("Alan M. Turing");
        window.findButton().withText("Save").click();

        assertEquals("Saved Alan M. Turing", window.findNotification().getText());
        assertEquals("Alan M. Turing", people.getCellText(row, 0));
    }
}
// --8<-- [end:test]
