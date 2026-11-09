package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** Unified API: runs on every Testing Tool. Shown in docs/comparison.md. */
// --8<-- [start:test]
@UnifiedSpringBootTest
class EditPersonUnifiedTest {

    @Test
    void editsAPersonFromTheGrid(UnifiedPage page) {
        page.navigate(PeopleView.class);
        UnifiedGrid people = page.find(UnifiedGrid.class).single();

        people.rows().withCellText("Name", "Alan Turing").single().click();
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alan M. Turing");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alan M. Turing");
        assertThat(people.rows().withCellText("Name", "Alan M. Turing").single())
                .hasCellText("Email", "alan@example.com");
    }
}
// --8<-- [end:test]
