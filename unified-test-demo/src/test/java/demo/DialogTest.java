package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedDialog;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

@UnifiedSpringBootTest
class DialogTest {

    // --8<-- [start:helpers]
    private UnifiedDialog openDialog(UnifiedPage page) {
        page.navigate(ProfileView.class);
        page.find(UnifiedButton.class).withText("Change email").single().click();
        return page.find(UnifiedDialog.class).withHeaderTitle("Change email").single();
    }
    // --8<-- [end:helpers]

    @Test
    void opensWithHeaderTitle(UnifiedPage page) {
        UnifiedDialog dialog = openDialog(page);

        assertThat(dialog).isOpen().isVisible().hasHeaderTitle("Change email");
    }

    // --8<-- [start:searchesInsideTheDialog]
    @Test
    void searchesInsideTheDialog(UnifiedPage page) {
        UnifiedDialog dialog = openDialog(page);

        // the page has its own "Email" field; scoping picks the dialog's one
        dialog.find(UnifiedTextField.class).withLabel("Email").single().setValue("new@example.com");
        dialog.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(dialog).isClosed();
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Email changed to new@example.com");
        assertThat(page.find(UnifiedTextField.class).withLabel("Email").single()).hasValue("new@example.com");
    }
    // --8<-- [end:searchesInsideTheDialog]

    @Test
    void closesWithCancel(UnifiedPage page) {
        UnifiedDialog dialog = openDialog(page);

        dialog.find(UnifiedButton.class).withText("Cancel").single().click();

        assertThat(dialog).isClosed();
    }

    @Test
    void closesWithEscape(UnifiedPage page) {
        UnifiedDialog dialog = openDialog(page);

        dialog.close();

        assertThat(dialog).isClosed();
    }
}
