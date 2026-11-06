package proto.a;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedTextField;

class UsersViewTest {

    void scenario(UnifiedPage page) {
        UnifiedGrid users = page.find(UnifiedGrid.class).withAccessibleName("Users").single();

        // filter in the header
        users.header("Name").find(UnifiedTextField.class).single().setValue("Ali");
        assert users.getRowCount() == 1;
        assert users.getCellText(0, "Email").equals("alice@example.com");

        // component in a cell
        users.cell(0, "Actions").find(UnifiedButton.class).withText("Edit").single().click();

        // selection, sorting, details
        users.select(0);
        assert users.isSelected(0);
        users.sort("Name");
        users.toggleDetails(0);
        users.details(0).find(UnifiedButton.class).withText("Reset password").single().click();
    }
}
