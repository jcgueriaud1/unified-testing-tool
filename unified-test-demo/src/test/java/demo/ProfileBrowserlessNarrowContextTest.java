package demo;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/** Golden migration of a narrow Spring context: unified-test-rewrite turns this into ProfileBrowserlessNarrowContextMigratedTest. */
@ContextConfiguration(classes = ProfileService.class)
class ProfileBrowserlessNarrowContextTest extends SpringBrowserlessTest {

    @Test
    void savesTheProfile() {
        navigate(ProfileView.class);
        test($(TextField.class).withLabel("Name").single()).setValue("Alice");
        test($(TextField.class).withLabel("Email").single()).setValue("alice@example.com");
        test($(Button.class).withText("Save").single()).click();

        Assertions.assertEquals("Saved Alice", test($(Notification.class).single()).getText());
    }
}
