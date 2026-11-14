package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Dramafinder 1.x, without the Unified API: the golden source of the 1.x migration. */
@UnifiedSpringBootTest
class EditPersonDramafinder1MigratedTest {

    @Test
    void editsAPersonFromTheGrid(UnifiedPage page) {
        page.navigate("people");
        UnifiedGrid people = page.find(UnifiedGrid.class).single();

        // TODO unified: was findRowIndexesWithColumnText(0, ..).get(0): any column matches now; single() fails when several rows match
        UnifiedGrid.UnifiedGridRow alan = people.row(people.rows().withCellText("Alan Turing").single().getIndex());
        // TODO unified: was getCell("Name").click(); a row's click() clicks the row, not that cell
        alan.click();
        // TODO unified: was 1.x getByLabel(..), a case-insensitive substring and first match; tighten it to withLabel(..) once the test passes
        page.find(UnifiedTextField.class).withAccessibleNameContaining("Name").single().setValue("Alan M. Turing");
        // TODO unified: was 1.x getByText(..), a case-insensitive substring and first match; tighten it to withText(..) once the test passes
        page.find(UnifiedButton.class).withTextContaining("Save").single().click();

        // TODO unified: was getByText(..), a substring and first match; single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).withTextContaining("Saved Alan M. Turing").single()).isOpen();
        assertThat(alan).hasCellText("Name", "Alan M. Turing");
    }
}
