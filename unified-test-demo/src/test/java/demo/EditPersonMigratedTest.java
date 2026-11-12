package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** TestBench (Selenium), without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@UnifiedSpringBootTest
class EditPersonMigratedTest {

    @Test
    void editsAPersonFromTheGrid(UnifiedPage page) {
        page.navigate(PeopleView.class);
        // TODO unified: was waitForFirst(); single() fails when several components match
        UnifiedGrid people = page.find(UnifiedGrid.class).single();

        // TODO unified: was getCell("Alan Turing"); single() fails when several rows match
        UnifiedGrid.UnifiedGridRow alan = people.rows().withCellText("Alan Turing").single();
        int row = alan.getIndex();
        alan.click();
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alan M. Turing");
        page.find(UnifiedButton.class).withText("Save").single().click();

        // TODO unified: was waitForFirst(); single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alan M. Turing");
        Assertions.assertEquals("Alan M. Turing", people.row(row).getCellText(0));
    }
}
// --8<-- [end:test]
