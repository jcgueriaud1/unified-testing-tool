package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Dramafinder (Playwright), without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@UnifiedSpringBootTest
class EditPersonDramafinderMigratedTest {

    @Test
    void editsAPersonFromTheGrid(UnifiedPage page) {
        page.navigate(PeopleView.class);
        UnifiedGrid people = page.find(UnifiedGrid.class).single();

        // TODO unified: was findRowIndexesWithColumnText(0, ..).get(0): any column matches now; single() fails when several rows match
        UnifiedGrid.UnifiedGridRow alan = people.row(people.rows().withCellText("Alan Turing").single().getIndex());
        // TODO unified: was getCell("Name").click(); a row's click() clicks the row, not that cell
        alan.click();
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alan M. Turing");
        page.find(UnifiedButton.class).withText("Save").single().click();

        // TODO unified: was getByText(..), a substring and first match; single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).withTextContaining("Saved Alan M. Turing").single()).isOpen();
        assertThat(alan).hasCellText("Name", "Alan M. Turing");
    }
}
// --8<-- [end:test]
