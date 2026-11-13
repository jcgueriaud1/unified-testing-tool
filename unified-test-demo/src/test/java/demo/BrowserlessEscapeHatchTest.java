package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** Browserless Test's test base API on a Unified Page, as a migrated Browserless test uses it. */
@UnifiedSpringBootTest
class BrowserlessEscapeHatchTest {

    @Test
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void runsBrowserlessTestBaseCalls(UnifiedPage page) {
        BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);

        ProfileView view = browserless.navigate(ProfileView.class);
        assertSame(view, browserless.getCurrentView());

        browserless.test(browserless.$(TextField.class).withCaption("Name").single()).setValue("Alice");
        browserless.test(browserless.$view(TextField.class).withCaption("Email").single()).setValue("alice@example.com");
        browserless.test(browserless.$(Button.class).withText("Save").single()).click();
        browserless.fireShortcut(Key.ESCAPE);
        browserless.roundTrip();
        browserless.runPendingSignalsTasks();

        // Unified API and Escape Hatch see the same window
        assertThat(page.find(UnifiedTextField.class).withLabel("Name").single()).hasValue("Alice");
        assertThat(page.find(UnifiedNotification.class).single()).hasTextContaining("Alice");
        assertEquals("Alice", browserless.$(TextField.class).withCaption("Name").single().getValue());
    }

    @Test
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void followsTheWindowTheUnifiedPageUses(UnifiedPage page) {
        UnifiedPage second = page.openWindow();
        second.navigate(ProfileView.class);
        page.navigate(ProfileView.class);
        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("First");

        BrowserlessEscapeHatch secondWindow = BrowserlessEscapeHatch.of(second);
        assertEquals("", secondWindow.$(TextField.class).withCaption("Name").single().getValue());
        UI secondUi = secondWindow.context().getUI();
        // and back: the Unified API finds the first window's field again
        assertThat(page.find(UnifiedTextField.class).withLabel("Name").single()).hasValue("First");
        // view code and testers read UI.getCurrent(): the Escape Hatch makes its window the current one
        secondWindow.context();
        assertSame(secondUi, UI.getCurrent());
        assertEquals("First", BrowserlessEscapeHatch.of(page).$(TextField.class).withCaption("Name").single()
                .getValue());
    }

    @Test
    @RequiresTestingTool({ TestingTool.PLAYWRIGHT, TestingTool.SELENIUM })
    void failsInABrowserNamingTheAnnotation(UnifiedPage page) {
        Exception failure = assertThrows(RuntimeException.class, () -> BrowserlessEscapeHatch.of(page));
        assertTrue(failure.getMessage().contains("RequiresTestingTool"), failure.getMessage());
    }
}
