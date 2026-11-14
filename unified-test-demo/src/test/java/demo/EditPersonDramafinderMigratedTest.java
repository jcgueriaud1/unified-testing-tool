package demo;

import com.microsoft.playwright.Page;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.dramafinder.element.GridElement;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

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
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alan M. Turing");
        page.find(UnifiedButton.class).withText("Save").single().click();

        // TODO unified: was getByText(..), a substring and first match; single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).withTextContaining("Saved Alan M. Turing").single()).isOpen();
        // TODO unified: Dramafinder Escape Hatch: alan.getCell("Name").assertText("Alan M. Turing")
        alan.getCell("Name").assertText("Alan M. Turing");
    }
}
// --8<-- [end:test]
