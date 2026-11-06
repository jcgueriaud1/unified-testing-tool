// PROTOTYPE V1 scenario. Same scenario in v2/ and v3/; compare how lookups read.
package proto.v1;

import static proto.common.UnifiedAssertions.assertThat;

import com.microsoft.playwright.Locator;
import proto.common.Annotations.RequiresBrowser;
import proto.common.Annotations.Test;
import proto.common.Annotations.UnifiedUITest;
import proto.common.Components.Button;
import proto.v1.Api.Dialog;
import proto.v1.Api.TextField;
import proto.v1.Api.UI;

@UnifiedUITest
class ProfileViewTest {

    @Test
    void savesName(UI ui) {
        ui.navigate("profile");

        TextField name = ui.findTextField().withLabel("Name").single();
        name.setValue("Alice");
        ui.findButton().withText("Save").single().click();

        assertThat(ui.findNotification().single()).isOpen().hasText("Saved Alice");
        assertThat(name).hasValue("Alice").isValid();
    }

    @Test
    void requiredEmailShowsError(UI ui) {
        ui.navigate("profile");

        ui.findButton().withText("Save").single().click();

        assertThat(ui.findTextField().withLabel("Email").single())
                .isInvalid()
                .hasErrorMessage("Email is required");
    }

    @Test
    void iconButtonAndDialogScope(UI ui) {
        ui.navigate("profile");

        // icon-only button: found by accessible name (aria-label), works on all Testing Tools
        ui.findButton().withAccessibleName("Delete account").single().click();

        Dialog confirm = ui.findDialog().withHeaderTitle("Delete account?").single();
        Button ok = confirm.findButton().withText("Delete").single();   // scoped search
        ok.click();

        assertThat(confirm).isClosed();
        // ui.findButton().withLabel("Delete");   // does not compile: Button has no label (ADR 0001)
    }

    @Test
    void layoutScopeByTestId(UI ui) {
        ui.navigate("profile");
        var address = ui.findContainer().withTestId("address-form").single();
        address.findTextField().withLabel("City").single().setValue("Turku");
    }

    @Test
    void escapeHatchExtras(UI ui) {
        ui.navigate("profile");
        TextField name = ui.findTextField().withLabel("Name").single();

        // optional tool-specific check, still runs (without it) on other Testing Tools
        name.tryUnwrap(Locator.class)
            .ifPresent(locator -> locator.evaluate("e => getComputedStyle(e).color"));
    }

    @Test
    @RequiresBrowser
    void screenshotNeedsABrowser(UI ui) {
        ui.navigate("profile");
        // skipped on Browserless (decided in #11); screenshot/CSS assertions shape still open
    }
}
