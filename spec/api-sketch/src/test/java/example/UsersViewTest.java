package example;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.UnifiedUITest;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedTextField;
import org.junit.jupiter.api.Test;

/** The spec's Grid example. Compiles against the sketch; never run. */
@UnifiedUITest
class UsersViewTest {

    static class UsersView extends Div {}   // stands in for an application @Route view

    @Test
    void editsAlice(UnifiedPage page) {
        page.navigate(UsersView.class);
        UnifiedGrid users = page.find(UnifiedGrid.class).withAccessibleName("Users").single();

        users.column("Name").header().find(UnifiedTextField.class).single().setValue("Ali");
        assertThat(users).hasRowCount(1);

        var alice = users.rows().withCellText("Name", "Alice").single();
        assertThat(alice).hasCellText("Email", "alice@example.com");
        alice.cell("Actions").find(UnifiedButton.class).withText("Edit").single().click();

        alice.select();
        assertThat(alice).isSelected();
        alice.toggleDetails();
        alice.details().find(UnifiedButton.class).withText("Reset password").single().click();
    }

    @Test
    void sortsByName(UnifiedPage page) {
        page.navigate(UsersView.class);
        UnifiedGrid users = page.find(UnifiedGrid.class).single();

        users.column("Name").sort(SortDirection.ASCENDING);
        assertThat(users.column("Name")).isSortedAscending().hasCellTexts("Alice", "Bob", "Carol");
        assertThat(users.row(0)).hasCellText("Name", "Alice");
    }
}
