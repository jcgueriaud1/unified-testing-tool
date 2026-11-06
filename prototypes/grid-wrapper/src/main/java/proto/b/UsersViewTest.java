package proto.b;

import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedTextField;
import java.util.List;

class UsersViewTest {

    void scenario(UnifiedPage page) {
        UnifiedGrid users = page.find(UnifiedGrid.class).withAccessibleName("Users").single();

        // filter in the header
        users.column("Name").header().find(UnifiedTextField.class).single().setValue("Ali");
        assert users.getRowCount() == 1;

        // find the row by what it shows, not by position
        UnifiedGrid.UnifiedGridRow alice = users.rows().withCellText("Name", "Alice").single();
        assert alice.getCellText("Email").equals("alice@example.com");

        // component in a cell
        alice.cell("Actions").find(UnifiedButton.class).withText("Edit").single().click();

        // selection, sorting, details
        alice.select();
        assert alice.isSelected();
        users.column("Name").sort(SortDirection.ASCENDING);
        assert users.column("Name").getCellTexts().equals(List.of("Alice", "Bob"));
        alice.toggleDetails();
        alice.details().find(UnifiedButton.class).withText("Reset password").single().click();

        // index access is still there when order matters
        assert users.row(0).getCellText("Name").equals("Alice");
    }
}
