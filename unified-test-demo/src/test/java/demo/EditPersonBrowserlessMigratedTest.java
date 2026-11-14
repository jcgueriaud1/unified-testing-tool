package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.grid.GridLocator;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Browserless Test, without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@UnifiedSpringBootTest
class EditPersonBrowserlessMigratedTest {

    @Test
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void editsAPersonFromTheGrid(UnifiedPage page) {
        BrowserlessUIContext window = page.unwrap(BrowserlessUIContext.class);
        page.navigate(PeopleView.class);
        // TODO unified: Browserless Escape Hatch: window.findGrid(PeopleView.Person.class)
        GridLocator<PeopleView.Person> people = window.findGrid(PeopleView.Person.class);

        int row = IntStream.range(0, people.size())
                .filter(i -> people.getCellText(i, 0).equals("Alan Turing"))
                .findFirst().orElseThrow();
        people.clickRow(row);
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alan M. Turing");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alan M. Turing");
        assertEquals("Alan M. Turing", people.getCellText(row, 0));
    }
}
// --8<-- [end:test]
