package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedContainer;
import com.vaadin.testing.unified.component.UnifiedDialog;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** Keyboard, DOM events and reload; written once, runs on Browserless (simulated) and Playwright. */
@UnifiedSpringBootTest
class KeyboardTest {

    @Test
    void enterInAFieldTriggersItsKeyListener(UnifiedPage page) {
        page.navigate(KeyboardView.class);
        UnifiedTextField search = page.find(UnifiedTextField.class).withLabel("Search").single();
        search.setValue("vaadin");

        search.element().pressKey("Enter");

        assertThat(page.find(UnifiedNotification.class).withText("Searching vaadin").single()).isOpen();
    }

    @Test
    void enterInAFormClicksItsShortcutButton(UnifiedPage page) {
        page.navigate(KeyboardView.class);
        UnifiedTextField comment = page.find(UnifiedTextField.class).withLabel("Comment").single();
        comment.setValue("Hello");

        comment.element().pressKey("Enter");

        assertThat(page.find(UnifiedNotification.class).withText("Sent Hello").single()).isOpen();
    }

    @Test
    void shortcutWithModifier(UnifiedPage page) {
        page.navigate(KeyboardView.class);

        page.find(UnifiedTextField.class).withLabel("Search").single().element().pressKey("Control+s");

        assertThat(page.find(UnifiedNotification.class).withText("Saved with Ctrl+S").single()).isOpen();
    }

    @Test
    void escapeOnThePageClosesTheDialog(UnifiedPage page) {
        page.navigate(KeyboardView.class);
        page.find(UnifiedButton.class).withText("Open dialog").single().click();
        UnifiedDialog dialog = page.find(UnifiedDialog.class).withHeaderTitle("Escape me").single();
        assertThat(dialog).isOpen();

        page.pressKey("Escape");

        assertThat(dialog).isClosed();
    }

    @Test
    void dispatchesACustomDomEvent(UnifiedPage page) {
        page.navigate(KeyboardView.class);

        page.find(UnifiedContainer.class).withTestId("drop-zone").single().element().dispatchEvent("item-dropped");

        assertThat(page.find(UnifiedNotification.class).withText("Item dropped").single()).isOpen();
    }

    @Test
    void reloadCreatesTheViewAgain(UnifiedPage page) {
        page.navigate(DraftView.class);
        page.find(UnifiedTextField.class).withLabel("Draft").single().setValue("unsaved");

        page.reload();

        assertThat(page).isAt(DraftView.class);
        assertThat(page.find(UnifiedTextField.class).withLabel("Draft").single()).isEmpty();
    }

    @Test
    void reloadKeepsAPreservedView(UnifiedPage page) {
        page.navigate(PreservedDraftView.class);
        page.find(UnifiedTextField.class).withLabel("Draft").single().setValue("kept");

        page.reload();

        assertThat(page).isAt(PreservedDraftView.class);
        assertThat(page.find(UnifiedTextField.class).withLabel("Draft").single()).hasValue("kept");
    }
}
