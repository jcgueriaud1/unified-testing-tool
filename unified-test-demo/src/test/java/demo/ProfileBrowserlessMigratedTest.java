package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedDialog;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration: unified-test-rewrite turns ProfileBrowserlessTest into ProfileBrowserlessMigratedTest. */
// --8<-- [start:test]
@UnifiedSpringBootTest
class ProfileBrowserlessMigratedTest {

    void open(UnifiedPage page) {
        page.navigate(ProfileView.class);
    }

    @Test
    void savesTheProfile(UnifiedPage page) {
        open(page);
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");
        // TODO unified: was first(); single() fails when several components match
        UnifiedTextField email = page.find(UnifiedTextField.class).withLabel("Email").single();
        email.setValue("alice@example.com");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedTextField.class).withLabel("Name").single()).hasValue("Alice");
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alice");
        assertThat(page.find(UnifiedButton.class).withText("Archive").single()).isDisabled();
    }

    @Test
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void opensTheEmailDialog(UnifiedPage page) {
        BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
        open(page);
        page.find(UnifiedButton.class).withText("Change email").single().click();

        assertThat(page.find(UnifiedDialog.class).single()).isOpen();
        // TODO unified: was fireShortcut(..); pressKey also reaches the focused component and an open dialog
        page.pressKey("Enter");

        assertThat(page).isAt(ProfileView.class);
        // TODO unified: Browserless Escape Hatch: test($(Button.class).withText("Archive").single()).isUsable()
        Assertions.assertFalse(browserless.test(browserless.$(Button.class).withText("Archive").single()).isUsable());
    }
}
// --8<-- [end:test]
