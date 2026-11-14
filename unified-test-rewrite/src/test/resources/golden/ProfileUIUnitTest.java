package demo;

import com.vaadin.testbench.unit.SpringUIUnitTest;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Golden migration: unified-test-rewrite turns ProfileBrowserlessTest into ProfileBrowserlessMigratedTest. */
@SpringBootTest
class ProfileUIUnitTest extends SpringUIUnitTest {

    @BeforeEach
    void open() {
        navigate(ProfileView.class);
    }

    @Test
    void savesTheProfile() {
        test($(TextField.class).withCaption("Name").single()).setValue("Alice");
        TextField email = $(TextField.class).withCaption("Email").first();
        test(email).setValue("alice@example.com");
        test($(Button.class).withText("Save").single()).click();
        roundTrip();

        Assertions.assertEquals("Alice", $(TextField.class).withCaption("Name").single().getValue());
        Assertions.assertEquals("Saved Alice", test($(Notification.class).single()).getText());
        Assertions.assertFalse($(Button.class).withText("Archive").single().isEnabled());
    }

    @Test
    void opensTheEmailDialog() {
        test($(Button.class).withText("Change email").single()).click();

        Assertions.assertTrue($(Dialog.class).single().isOpened());
        fireShortcut(Key.ENTER);

        Assertions.assertTrue(getCurrentView() instanceof ProfileView);
        Assertions.assertFalse(test($(Button.class).withText("Archive").single()).isUsable());
    }
}
