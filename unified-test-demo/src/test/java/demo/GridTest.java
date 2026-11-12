package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnifiedSpringBootTest
class GridTest {

    private static UnifiedGrid grid(UnifiedPage page, String testId) {
        page.navigate(UsersView.class);
        return page.find(UnifiedGrid.class).withTestId(testId).single();
    }

    @Test
    void readsCountHeadersAndCells(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        assertThat(users).hasRowCount(120);
        assertEquals(List.of("Name", "Email", "Role", "Actions"), users.getColumnHeaders());
        assertThat(users.row(0)).hasCellText("Name", "User 1").hasCellText("Email", "user1@example.com");
    }

    @Test
    void findsARowByWhatItShows(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        // far beyond the first page: loaded lazily
        UnifiedGrid.UnifiedGridRow row = users.rows().withCellText("Name", "User 110").single();

        assertEquals(109, row.getIndex());
        assertThat(row).hasCellText("Email", "user110@example.com").hasCellText("Role", "Admin");
    }

    @Test
    void findsARowByCellTextInAnyColumn(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        // "user110@example.com" is in the Email column, "User 110" in the Name column
        assertEquals(109, users.rows().withCellText("user110@example.com").single().getIndex());
        assertEquals(109, users.rows().withCellText("User 110").single().getIndex());
        assertFalse(users.rows().withCellText("user110@example").exists());
    }

    @Test
    void readsACellByColumnIndex(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        assertEquals("User 1", users.row(0).getCellText(0));
        assertEquals("user110@example.com", users.rows().withCellText("Name", "User 110").single().getCellText(1));
    }

    @Test
    void combinesCellFilters(UnifiedPage page) {
        UnifiedGrid users = grid(page, "users");

        // "user11" matches user11 and user110..user119 (11 rows); only user110 of them is an Admin
        assertEquals(11, users.rows().withCellTextContaining("Email", "user11").all().size());
        // ...Containing ignores case
        assertEquals(1, users.rows().withCellTextContaining("Email", "USER11@").all().size());
        assertEquals(1, users.rows().withCellTextContaining("Email", "user11").withCellText("Role", "Admin").all().size());
        assertFalse(users.rows().withCellText("Name", "Nobody").exists());
    }

    @Test
    void readsColumnsByHeaderIdAndIndex(UnifiedPage page) {
        UnifiedGrid roles = grid(page, "roles");

        assertThat(roles.column("Role")).hasCellTexts("Admin", "Editor", "Viewer");
        assertThat(roles.columnWithId("owner")).hasCellTexts("Ada", "Bob", "Cy");
        assertThat(roles.column(1)).hasCellTexts("Ada", "Bob", "Cy");
    }

    @Test
    void knowsAnEmptyGrid(UnifiedPage page) {
        assertThat(grid(page, "archived")).isEmpty();
    }
}
