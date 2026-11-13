package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedTextField;

/** Golden migration of a TestBench base type, a test extending it and a page object (includePageObjects). */
class ProfileMigratedPage {

    private final UnifiedPage test;

    ProfileMigratedPage(UnifiedPage test) {
        this.test = test;
    }

    void fill(String name, String email) {
        test.find(UnifiedTextField.class).withLabel("Name").single().setValue(name);
        test.find(UnifiedTextField.class).withLabel("Email").single().setValue(email);
    }

    UnifiedButton saveButton() {
        return test.find(UnifiedButton.class).withText("Save").single();
    }
}
