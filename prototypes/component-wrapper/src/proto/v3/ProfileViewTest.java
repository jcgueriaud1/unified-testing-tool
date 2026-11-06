// PROTOTYPE V3 scenario (chosen). Same scenario as v1/ and v2/.
package proto.v3;

import static proto.common.UnifiedAssertions.assertThat;

import com.microsoft.playwright.Locator;
import proto.common.Annotations.RequiresBrowser;
import proto.common.Annotations.Test;
import proto.common.Annotations.UnifiedUITest;
import proto.v3.Api.UI;
import proto.v3.Api.UnifiedButton;
import proto.v3.Api.UnifiedContainer;
import proto.v3.Api.UnifiedDialog;
import proto.v3.Api.UnifiedNotification;
import proto.v3.Api.UnifiedTextField;

@UnifiedUITest
class ProfileViewTest {

    @Test
    void savesName(UI ui) {
        ui.navigate("profile");

        UnifiedTextField name = ui.find(UnifiedTextField.class).withLabel("Name").single();
        name.setValue("Alice");
        ui.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(ui.find(UnifiedNotification.class).single()).isOpen().hasText("Saved Alice");
        assertThat(name).hasValue("Alice").isValid();
    }

    @Test
    void requiredEmailShowsError(UI ui) {
        ui.navigate("profile");

        ui.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(ui.find(UnifiedTextField.class).withLabel("Email").single())
                .isInvalid()
                .hasErrorMessage("Email is required");
    }

    @Test
    void iconButtonAndDialogScope(UI ui) {
        ui.navigate("profile");

        ui.find(UnifiedButton.class).withAccessibleName("Delete account").single().click();

        UnifiedDialog confirm = ui.find(UnifiedDialog.class).withHeaderTitle("Delete account?").single();
        confirm.find(UnifiedButton.class).withText("Delete").single().click();   // scoped search

        assertThat(confirm).isClosed();
        // ui.find(UnifiedButton.class).withLabel("Delete");   // does not compile: Button has no label (ADR 0001)
    }

    @Test
    void layoutScopeByTestId(UI ui) {
        ui.navigate("profile");
        var address = ui.find(UnifiedContainer.class).withTestId("address-form").single();
        address.find(UnifiedTextField.class).withLabel("City").single().setValue("Turku");
    }

    @Test
    void escapeHatchExtras(UI ui) {
        ui.navigate("profile");
        var name = ui.find(UnifiedTextField.class).withLabel("Name").single();
        // no import clash with either tool's TextFieldElement
        name.tryUnwrap(Locator.class)
            .ifPresent(locator -> locator.evaluate("e => getComputedStyle(e).color"));
    }

    @Test
    @RequiresBrowser
    void screenshotNeedsABrowser(UI ui) {
        ui.navigate("profile");
    }
}
