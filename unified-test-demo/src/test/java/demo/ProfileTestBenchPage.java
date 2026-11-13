package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.HasElementQuery;

/** Golden migration of a TestBench base type, a test extending it and a page object (includePageObjects). */
class ProfileTestBenchPage {

    private final HasElementQuery test;

    ProfileTestBenchPage(HasElementQuery test) {
        this.test = test;
    }

    void fill(String name, String email) {
        test.$(TextFieldElement.class).withLabel("Name").single().setValue(name);
        test.$(TextFieldElement.class).withLabel("Email").single().setValue(email);
    }

    ButtonElement saveButton() {
        return test.$(ButtonElement.class).withText("Save").single();
    }
}
