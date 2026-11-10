package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.RequiresBrowser;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

@UnifiedSpringBootTest
class GridInteractionTest {

    private static UnifiedGrid grid(UnifiedPage page, String testId) {
        page.navigate(UsersView.class);
        return page.find(UnifiedGrid.class).withTestId(testId).single();
    }

    private static void assertNotification(UnifiedPage page, String text) {
        assertThat(page.find(UnifiedNotification.class).withText(text).single()).isOpen();
    }

    @Test
    void clicksAComponentInACell(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        users.rows().withCellText("Name", "User 3").single()
                .cell("Actions").find(UnifiedButton.class).withText("Edit").single().click();

        assertNotification(page, "Editing User 3");
    }

    /** A page-wide find reaches components rendered in grid cells, as in a browser. */
    @Test
    void findsACellComponentPageWide(UnifiedPage page) {
        grid(page, "users");

        page.find(UnifiedButton.class).withAccessibleName("Edit User 3").single().click();

        assertNotification(page, "Editing User 3");
    }

    @Test
    void filtersThroughAHeaderField(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        users.column("Name").header().find(UnifiedTextField.class).withPlaceholder("Filter by name").single()
                .setValue("User 11");

        // User 11 and User 110..119
        assertThat(users).hasRowCount(11);
        assertThat(users.row(0)).hasCellText("Name", "User 11");
    }

    @Test
    void sortsAColumn(UnifiedPage page) {
        UnifiedGrid roles = grid(page, "roles");

        roles.column("Role").sort(SortDirection.DESCENDING);
        assertThat(roles.column("Role")).isSortedDescending().hasCellTexts("Viewer", "Editor", "Admin");

        roles.column("Role").sort(SortDirection.ASCENDING);
        assertThat(roles.column("Role")).isSortedAscending().hasCellTexts("Admin", "Editor", "Viewer");
    }

    @Test
    void selectsRows(UnifiedPage page) {
        UnifiedGrid roles = grid(page, "roles");

        // the multi-select checkbox column is not a Unified column
        assertThat(roles.column(1)).hasCellTexts("Ada", "Bob", "Cy");

        UnifiedGrid.UnifiedGridRow bob = roles.rows().withCellText("Role", "Editor").single();
        bob.select();
        assertThat(bob).isSelected();
        assertEquals(1, roles.rows().selected().all().size());

        roles.selectAll();
        assertEquals(3, roles.rows().selected().all().size());

        roles.deselectAll();
        assertThat(bob).isNotSelected();
    }

    @Test
    void clicksAndDoubleClicksRows(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        users.row(1).click();
        assertNotification(page, "Clicked User 2");

        users.row(4).doubleClick();
        assertNotification(page, "Opened User 5");
    }

    @Test
    void togglesRowDetails(UnifiedPage page) {
        UnifiedGrid.UnifiedGridRow row = grid(page, "users").row(2);

        row.toggleDetails();
        assertThat(row).hasDetailsOpen();

        row.toggleDetails();
        assertThat(row).hasDetailsClosed();
    }

    @Test
    @RequiresBrowser // Browserless can't search inside row details yet (gap ticket)
    void clicksAButtonInRowDetails(UnifiedPage page) {
        UnifiedGrid.UnifiedGridRow row = grid(page, "users").row(2);

        row.toggleDetails();
        row.details().find(UnifiedButton.class).withText("Reset password").single().click();

        assertNotification(page, "Password reset for User 3");
    }
}
