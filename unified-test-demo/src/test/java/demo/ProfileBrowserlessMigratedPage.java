package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;

/** A Browserless page object; GoldenMigrationTest migrates it, with includePageObjects, together with its test. */
// --8<-- [start:page]
class ProfileBrowserlessMigratedPage {

    private final UnifiedPage test;

    ProfileBrowserlessMigratedPage(UnifiedPage test) {
        this.test = test;
    }

    void open() {
        test.navigate(ProfileView.class);
    }

    void save(String name, String email) {
        test.find(UnifiedTextField.class).withLabel("Name").single().setValue(name);
        test.find(UnifiedTextField.class).withLabel("Email").single().setValue(email);
        test.find(UnifiedButton.class).withText("Save").single().click();
    }

    UnifiedNotification notification() {
        return test.find(UnifiedNotification.class).single();
    }
}
// --8<-- [end:page]
