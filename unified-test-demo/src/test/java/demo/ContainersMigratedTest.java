package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedDialog;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTabSheet;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration of Dialog and TabSheet: unified-test-rewrite turns ContainersTestBenchTest into ContainersMigratedTest. */
@UnifiedSpringBootTest
class ContainersMigratedTest {

    @Test
    void changesTheEmailInTheDialog(UnifiedPage page) {
        page.navigate(ProfileView.class);

        page.find(UnifiedButton.class).withText("Change email").single().click();
        // TODO unified: was waitForFirst(); single() fails when several components match
        UnifiedDialog dialog = page.find(UnifiedDialog.class).single();
        assertThat(dialog).hasHeaderTitle("Change email");
        dialog.find(UnifiedTextField.class).withLabel("Email").single().setValue("bob@example.com");
        dialog.find(UnifiedButton.class).withText("Save").single().click();

        // TODO unified: was waitForFirst(); single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Email changed to bob@example.com");
    }

    @Test
    void changesThePasswordInATab(UnifiedPage page) {
        page.navigate(AccountView.class);

        UnifiedTabSheet tabs = page.find(UnifiedTabSheet.class).single();
        assertThat(tabs).hasTabs("Profile", "Security", "Billing");
        tabs.selectTab("Security");
        tabs.content().find(UnifiedButton.class).withText("Change password").single().click();

        assertThat(tabs).hasSelectedTab("Security");
    }
}
