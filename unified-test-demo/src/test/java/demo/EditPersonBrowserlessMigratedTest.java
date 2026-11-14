package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** Browserless Test, without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@UnifiedSpringBootTest
class EditPersonBrowserlessMigratedTest {

    @Test
    void editsAPersonFromTheGrid(UnifiedPage page) {
        page.navigate(PeopleView.class);
        UnifiedGrid people = page.find(UnifiedGrid.class).single();

        // TODO unified: was a scan of column 0; withCellText(text) matches any column: name the column, withCellText(header, text), if another column can hold the text
        int row = people.rows().withCellText("Alan Turing").single().getIndex();
        people.row(row).click();
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alan M. Turing");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alan M. Turing");
        assertEquals("Alan M. Turing", people.row(row).getCellText(0));
    }
}
// --8<-- [end:test]
