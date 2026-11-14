package demo;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Golden migration with leftovers: unified-test-rewrite turns this into ProfileBrowserlessLeftoversMigratedTest. */
@SpringBootTest
class ProfileBrowserlessLeftoversTest extends SpringBrowserlessTest {

    @Test
    void savesTheProfile() {
        ProfileView view = navigate(ProfileView.class);
        TextField name = (TextField) view.getChildren().findFirst().orElseThrow();
        test(name).setValue("Alice");
        test($(TextField.class).withCondition(TextField::isRequiredIndicatorVisible).single())
                .setValue("alice@example.com");
        test($(Button.class).withText("Save").single()).click();

        Assertions.assertEquals("Saved Alice", test($(Notification.class).single()).getText());
    }
}
