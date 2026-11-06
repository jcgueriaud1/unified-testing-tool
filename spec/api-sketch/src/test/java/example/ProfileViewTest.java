package example;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.flow.component.html.Div;
import com.vaadin.testing.unified.RequiresBrowser;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.UnifiedSpringBootTest;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedContainer;
import com.vaadin.testing.unified.component.UnifiedDialog;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The spec's reference example. Compiles against the sketch; never run. */
@UnifiedSpringBootTest
class ProfileViewTest {

    static class ProfileView extends Div {}   // stands in for an application @Route view

    @Test
    void savesName(UnifiedPage page) {
        page.navigate(ProfileView.class);

        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
        name.setValue("Alice");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).isOpen().hasText("Saved Alice");
        assertThat(name).hasValue("Alice").isValid();
    }

    @Test
    void deletesAccount(UnifiedPage page) {
        page.navigate(ProfileView.class);

        page.find(UnifiedButton.class).withAccessibleName("Delete account").single().click();
        UnifiedDialog dialog = page.find(UnifiedDialog.class).withHeaderTitle("Delete account?").single();
        dialog.find(UnifiedButton.class).withText("Delete").single().click();

        assertThat(dialog).isClosed();
        assertThat(page).hasPath("goodbye");
    }

    @Test
    void layoutScope(UnifiedPage page) {
        page.navigate("profile?tab=address");
        var address = page.find(UnifiedContainer.class).withTestId("address-form").single();
        address.find(UnifiedTextField.class).withLabel("City").single().setValue("Turku");
    }

    @Test
    @RequiresBrowser
    void looksRight(UnifiedPage page) {
        page.navigate(ProfileView.class);
        assertThat(page.find(UnifiedButton.class).withText("Save").single()).hasCssValue("cursor", "pointer");
        page.screenshot(Path.of("target/profile.png"));
    }
}
