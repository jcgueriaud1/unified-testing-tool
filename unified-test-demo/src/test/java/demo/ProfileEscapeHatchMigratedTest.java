package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration with leftovers: unified-test-rewrite turns this into ProfileEscapeHatchMigratedTest. */
@UnifiedSpringBootTest
class ProfileEscapeHatchMigratedTest {

    void open(UnifiedPage page) {
        page.navigate(ProfileView.class);
    }

    @Test
    void savesTheName(UnifiedPage page) {
        open(page);
        // TODO unified: was first(); single() fails when several components match
        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
        name.setValue("Alice");
        save(page);

        assertThat(name).hasValue("Alice");
    }

    @Test
    @RequiresTestingTool(TestingTool.SELENIUM)
    void knowsTheArchiveButtonIsDisabled(UnifiedPage page) {
        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
        open(page);
        // TODO unified: TestBench Escape Hatch: $(ButtonElement.class)
        Assertions.assertTrue(testBench.$(ButtonElement.class).withText("Archive").single().hasAttribute("disabled"));
        // TODO unified: TestBench Escape Hatch: executeScript("return document.location.pathname.substring(1)")
        Assertions.assertEquals("profile", testBench.executeScript("return document.location.pathname.substring(1)"));
    }

    private void save(UnifiedPage page) {
        page.find(UnifiedButton.class).withText("Save").single().click();
    }
}
