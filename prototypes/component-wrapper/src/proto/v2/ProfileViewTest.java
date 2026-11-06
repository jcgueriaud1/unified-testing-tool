// PROTOTYPE V2 scenario. Same scenario as v1/ and v3/.
package proto.v2;

import static proto.common.UnifiedAssertions.assertThat;

import com.microsoft.playwright.Locator;
import proto.common.Annotations.RequiresBrowser;
import proto.common.Annotations.Test;
import proto.common.Annotations.UnifiedUITest;
import proto.v2.Api.Button;
import proto.v2.Api.Container;
import proto.v2.Api.Dialog;
import proto.v2.Api.Notification;
import proto.v2.Api.TextField;
import proto.v2.Api.TextFieldSpec;
import proto.v2.Api.UI;

@UnifiedUITest
class ProfileViewTest {

    // specs are values: reusable, page-object friendly
    static final TextFieldSpec NAME = TextField.withLabel("Name");
    static final TextFieldSpec EMAIL = TextField.withLabel("Email");

    @Test
    void savesName(UI ui) {
        ui.navigate("profile");

        TextField name = ui.find(NAME);
        name.setValue("Alice");
        ui.find(Button.withText("Save")).click();

        assertThat(ui.find(Notification.any())).isOpen().hasText("Saved Alice");
        assertThat(name).hasValue("Alice").isValid();
    }

    @Test
    void requiredEmailShowsError(UI ui) {
        ui.navigate("profile");

        ui.find(Button.withText("Save")).click();

        assertThat(ui.find(EMAIL))
                .isInvalid()
                .hasErrorMessage("Email is required");
    }

    @Test
    void iconButtonAndDialogScope(UI ui) {
        ui.navigate("profile");

        ui.find(Button.withAccessibleName("Delete account")).click();

        Dialog confirm = ui.find(Dialog.withHeaderTitle("Delete account?"));
        confirm.find(Button.withText("Delete")).click();          // scoped search

        assertThat(confirm).isClosed();
        // Button.withLabel("Delete");   // does not compile: Button has no label (ADR 0001)
    }

    @Test
    void layoutScopeByTestId(UI ui) {
        ui.navigate("profile");
        Container address = ui.find(Container.withTestId("address-form"));
        address.find(TextField.withLabel("City")).setValue("Turku");
    }

    @Test
    void escapeHatchExtras(UI ui) {
        ui.navigate("profile");
        ui.find(NAME).tryUnwrap(Locator.class)
          .ifPresent(locator -> locator.evaluate("e => getComputedStyle(e).color"));
    }

    @Test
    @RequiresBrowser
    void screenshotNeedsABrowser(UI ui) {
        ui.navigate("profile");
    }
}
