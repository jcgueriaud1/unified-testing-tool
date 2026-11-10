package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.testing.unified.RequiresBrowser;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

/** Written once; runs on Browserless (default) and on Playwright (-Dvaadin.testing.tool=playwright). */
@UnifiedSpringBootTest
class ProfileViewTest {

    @Test
    void savesName(UnifiedPage page) {
        page.navigate(ProfileView.class);

        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
        name.setValue("Alice");
        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("alice@example.com");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).isOpen().hasText("Saved Alice");
        assertThat(name).hasValue("Alice").isValid();
    }

    @Test
    void requiresEmail(UnifiedPage page) {
        page.navigate(ProfileView.class);

        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedTextField.class).withLabel("Email").single())
                .isInvalid()
                .hasErrorMessage("Email is required");
    }

    @Test
    void findsIconButtonByAccessibleName(UnifiedPage page) {
        page.navigate(ProfileView.class);

        page.find(UnifiedButton.class).withAccessibleName("Delete account").single().click();

        assertThat(page.find(UnifiedNotification.class).withText("Account deleted").single()).isOpen();
    }

    @Test
    void findsByTestIdAndPlaceholder(UnifiedPage page) {
        page.navigate(ProfileView.class);

        page.find(UnifiedTextField.class).withTestId("nickname").single().setValue("Ali");

        assertThat(page.find(UnifiedTextField.class).withPlaceholder("Nickname").single()).hasValue("Ali");
    }

    @Test
    void knowsDisabledButtons(UnifiedPage page) {
        page.navigate(ProfileView.class);

        assertThat(page.find(UnifiedButton.class).withText("Archive").single()).isDisabled();
        assertThat(page.find(UnifiedButton.class).withText("Save").single()).isEnabled().hasText("Save");
    }

    @Test
    void escapeHatchIsOptional(UnifiedPage page) {
        page.navigate(ProfileView.class);
        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();

        // the Flow component exists only on Browserless; the test still runs everywhere
        name.tryUnwrap(TextField.class).ifPresent(field -> assertEquals("Name", field.getLabel()));
        assertTrue(page.find(UnifiedButton.class).withText("Save").exists());
        assertFalse(page.find(UnifiedButton.class).withText("No such button").exists());
    }

    @Test
    void unwrapNamesTheMissingAnnotation(UnifiedPage page) {
        page.navigate(ProfileView.class);
        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> name.unwrap(java.io.File.class));
        assertTrue(e.getMessage().contains("@RequiresTestingTool"), e.getMessage());
    }

    @Test
    @RequiresTestingTool(TestingTool.SELENIUM)
    void escapeHatchToTestBench(UnifiedPage page) {
        page.navigate(ProfileView.class);
        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();

        TextFieldElement testBench = name.unwrap(TextFieldElement.class);
        testBench.setValue("Carol");

        assertEquals("Name", testBench.getLabel());
        assertThat(name).hasValue("Carol");
        assertTrue(page.unwrap(WebDriver.class).getCurrentUrl().endsWith("/profile"));
    }

    @Test
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void readsServerSideState(UnifiedPage page) {
        page.navigate(ProfileView.class);
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Bob");

        TextField field = page.find(UnifiedTextField.class).withLabel("Name").single().unwrap(TextField.class);
        assertEquals("Bob", field.getValue());
    }

    @Test
    @RequiresBrowser
    void needsARealBrowser(UnifiedPage page) {
        page.navigate(ProfileView.class);
        assertThat(page.find(UnifiedButton.class).withText("Save").single()).hasCssValue("cursor", "pointer");
    }

    @Test
    void screenshotOnlyWithARealBrowser(UnifiedPage page) throws Exception {
        page.navigate(ProfileView.class);
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");

        Path file = Path.of("target/screenshots/profile-alice.png");
        Files.deleteIfExists(file);
        if (page.hasRealBrowser()) {
            page.screenshot(file);
        }

        assertEquals(page.hasRealBrowser(), Files.isRegularFile(file));
    }
}
