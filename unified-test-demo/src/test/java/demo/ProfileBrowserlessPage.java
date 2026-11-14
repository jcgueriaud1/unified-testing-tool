package demo;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;

/** A Browserless page object; GoldenMigrationTest migrates it, with includePageObjects, together with its test. */
// --8<-- [start:page]
class ProfileBrowserlessPage {

    private final SpringBrowserlessTest test;

    ProfileBrowserlessPage(SpringBrowserlessTest test) {
        this.test = test;
    }

    void open() {
        test.navigate(ProfileView.class);
    }

    void save(String name, String email) {
        test.test(test.$(TextField.class).withCaption("Name").single()).setValue(name);
        test.test(test.$(TextField.class).withCaption("Email").single()).setValue(email);
        test.test(test.$(Button.class).withText("Save").single()).click();
    }

    Notification notification() {
        return test.$(Notification.class).single();
    }
}
// --8<-- [end:page]
