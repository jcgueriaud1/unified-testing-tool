package demo;

import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

/** Dramafinder (Playwright), without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@UnifiedSpringBootTest
class EditPersonDramafinderMigratedTest {

    @Test
    @RequiresTestingTool(TestingTool.PLAYWRIGHT)
    void editsAPersonFromTheGrid(UnifiedPage page) {
        Page playwright = page.unwrap(Page.class);
        page.navigate(PeopleView.class);
        // TODO unified: Dramafinder Escape Hatch: GridElement.get(playwright)
        GridElement people = GridElement.get(playwright);

        // TODO unified: Dramafinder Escape Hatch: people.findRow(people.findRowIndexesWithColumnText(0, "Alan Turing").get(0))
        GridElement.RowElement alan = people.findRow(people.findRowIndexesWithColumnText(0, "Alan Turing").get(0))
                .orElseThrow();
        // TODO unified: Dramafinder Escape Hatch: alan.getCell("Name").click()
        alan.getCell("Name").click();
        // TODO unified: Dramafinder Escape Hatch: TextFieldElement.getByLabel(playwright, "Name").setValue("Alan M. Turing")
        TextFieldElement.getByLabel(playwright, "Name").setValue("Alan M. Turing");
        // TODO unified: Dramafinder Escape Hatch: ButtonElement.getByText(playwright, "Save").click()
        ButtonElement.getByText(playwright, "Save").click();

        // TODO unified: Dramafinder Escape Hatch: NotificationElement.getByText(playwright, "Saved Alan M. Turing").assertOpen()
        NotificationElement.getByText(playwright, "Saved Alan M. Turing").assertOpen();
        // TODO unified: Dramafinder Escape Hatch: alan.getCell("Name").assertText("Alan M. Turing")
        alan.getCell("Name").assertText("Alan M. Turing");
    }
}
// --8<-- [end:test]
