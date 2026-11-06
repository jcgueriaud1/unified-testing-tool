// PROTOTYPE V3 scenario. Same scenario as v1/ and v2/.
package proto.v3;

import static proto.common.UnifiedAssertions.assertThat;
import static proto.v3.Api.BUTTON;
import static proto.v3.Api.CONTAINER;
import static proto.v3.Api.DIALOG;
import static proto.v3.Api.NOTIFICATION;
import static proto.v3.Api.TEXT_FIELD;

import com.microsoft.playwright.Locator;
import proto.common.Annotations.RequiresBrowser;
import proto.common.Annotations.Test;
import proto.common.Annotations.UnifiedUITest;
import proto.v3.Api.Dialog;
import proto.v3.Api.TextField;
import proto.v3.Api.UI;

@UnifiedUITest
class ProfileViewTest {

    @Test
    void savesName(UI ui) {
        ui.navigate("profile");

        TextField name = ui.find(TEXT_FIELD).withLabel("Name").single();
        name.setValue("Alice");
        ui.find(BUTTON).withText("Save").single().click();

        assertThat(ui.find(NOTIFICATION).single()).isOpen().hasText("Saved Alice");
        assertThat(name).hasValue("Alice").isValid();
    }

    @Test
    void requiredEmailShowsError(UI ui) {
        ui.navigate("profile");

        ui.find(BUTTON).withText("Save").single().click();

        assertThat(ui.find(TEXT_FIELD).withLabel("Email").single())
                .isInvalid()
                .hasErrorMessage("Email is required");
    }

    @Test
    void iconButtonAndDialogScope(UI ui) {
        ui.navigate("profile");

        ui.find(BUTTON).withAccessibleName("Delete account").single().click();

        Dialog confirm = ui.find(DIALOG).withHeaderTitle("Delete account?").single();
        confirm.find(BUTTON).withText("Delete").single().click();   // scoped search

        assertThat(confirm).isClosed();
        // ui.find(BUTTON).withLabel("Delete");   // does not compile: Button has no label (ADR 0001)
    }

    @Test
    void layoutScopeByTestId(UI ui) {
        ui.navigate("profile");
        var address = ui.find(CONTAINER).withTestId("address-form").single();
        address.find(TEXT_FIELD).withLabel("City").single().setValue("Turku");
    }

    @Test
    void escapeHatchExtras(UI ui) {
        ui.navigate("profile");
        ui.find(TEXT_FIELD).withLabel("Name").single()
          .tryUnwrap(Locator.class)
          .ifPresent(locator -> locator.evaluate("e => getComputedStyle(e).color"));
    }

    @Test
    @RequiresBrowser
    void screenshotNeedsABrowser(UI ui) {
        ui.navigate("profile");
    }
}
