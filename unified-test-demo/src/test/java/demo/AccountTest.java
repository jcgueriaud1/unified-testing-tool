package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTabSheet;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** TabSheet; written once, runs on Browserless and Playwright. */
@UnifiedSpringBootTest
class AccountTest {

    @Test
    void switchesTabsAndWorksInside(UnifiedPage page) {
        page.navigate(AccountView.class);
        UnifiedTabSheet tabs = page.find(UnifiedTabSheet.class).single();
        assertThat(tabs).hasSelectedTab("Profile").hasTabs("Profile", "Security", "Billing");

        tabs.selectTab("Security");

        assertThat(tabs).hasSelectedTab("Security");
        assertThat(page.find(UnifiedNotification.class).withText("Opened Security").single()).isOpen();
        tabs.content().find(UnifiedButton.class).withText("Change password").single().click();
        assertThat(page.find(UnifiedNotification.class).withText("Password changed").single()).isOpen();
    }

    @Test
    void contentFollowsTheSelectedTab(UnifiedPage page) {
        page.navigate(AccountView.class);
        UnifiedTabSheet tabs = page.find(UnifiedTabSheet.class).withTab("Security").single();
        Scope content = tabs.content();

        content.find(UnifiedTextField.class).withLabel("Notes").single().setValue("profile notes");
        assertEquals(1, content.find(UnifiedTextField.class).withLabel("Name").all().size());

        tabs.selectTab("Security");

        // same Scope object, now the Security tab: one "Notes" field, empty, and no "Name"
        assertThat(content.find(UnifiedTextField.class).withLabel("Notes").single()).isEmpty();
        assertEquals(0, content.find(UnifiedTextField.class).withLabel("Name").all().size());
        assertEquals("Security", tabs.getSelectedTab());
    }

    @Test
    void unknownTabFailsWithTheTabs(UnifiedPage page) {
        page.navigate(AccountView.class);
        UnifiedTabSheet tabs = page.find(UnifiedTabSheet.class).single();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tabs.selectTab("Admin"));

        assertTrue(e.getMessage().contains("[Profile, Security, Billing]"), e.getMessage());
    }

    @Test
    void disabledTabCantBeSelected(UnifiedPage page) {
        page.navigate(AccountView.class);
        UnifiedTabSheet tabs = page.find(UnifiedTabSheet.class).single();

        AssertionError e = assertThrows(AssertionError.class,
                () -> ((UnifiedTabSheet) tabs.withTimeout(Duration.ofMillis(500))).selectTab("Billing"));

        assertTrue(e.getMessage().contains("the tab is disabled"), e.getMessage());
        assertThat(tabs).hasSelectedTab("Profile");
    }
}
