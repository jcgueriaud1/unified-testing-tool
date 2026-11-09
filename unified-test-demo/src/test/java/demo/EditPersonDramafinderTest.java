package demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;
import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

/** Dramafinder (Playwright), without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "playwright")
class EditPersonDramafinderTest extends SpringPlaywrightIT {

    @Test
    void editsAPersonFromTheGrid() {
        navigate(PeopleView.class);
        GridElement people = GridElement.get(page);

        int row = people.findRowIndexesWithColumnText(0, "Alan Turing").get(0);
        people.findCell(row, 0).orElseThrow().click();
        TextFieldElement.getByLabel(page, "Name").setValue("Alan M. Turing");
        ButtonElement.getByText(page, "Save").click();

        NotificationElement.getByText(page, "Saved Alan M. Turing").assertOpen();
        people.assertCellContent(row, 0, "Alan M. Turing");
    }
}
// --8<-- [end:test]
