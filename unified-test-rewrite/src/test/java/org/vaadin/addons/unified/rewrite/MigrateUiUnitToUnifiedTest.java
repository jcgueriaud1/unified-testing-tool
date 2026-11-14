package org.vaadin.addons.unified.rewrite;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/** TestBench UI Unit 9.x tests migrate through Browserless Test's rules (ADR 0005, #141). */
class MigrateUiUnitToUnifiedTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateBrowserlessToUnified()).parser(UiUnit.parser());
    }

    @Test
    void aUiUnitTestBecomesAUnifiedUITest() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.button.Button;
                import com.vaadin.flow.component.textfield.TextField;
                import com.vaadin.testbench.unit.UIUnitTest;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class ProfileTest extends UIUnitTest {
                    @Test
                    void saves() {
                        test($(TextField.class).withCaption("Name").single()).setValue("Alice");
                        test($(Button.class).withText("Save").single()).click();
                        Assertions.assertEquals("Alice", $(TextField.class).withCaption("Name").single().getValue());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                @UnifiedUITest
                class ProfileTest {
                    @Test
                    void saves(UnifiedPage page) {
                        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");
                        page.find(UnifiedButton.class).withText("Save").single().click();
                        assertThat(page.find(UnifiedTextField.class).withLabel("Name").single()).hasValue("Alice");
                    }
                }
                """));
    }

    @Test
    void aJUnit4UiUnitTestMigratesToJUnit6() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.button.Button;
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.testbench.unit.UIUnit4Test;
                import org.junit.Before;
                import org.junit.Test;

                public class ProfileTest extends UIUnit4Test {
                    @Before
                    public void open() {
                        navigate(Div.class);
                    }

                    @Test
                    public void saves() {
                        test($(Button.class).withText("Save").single()).click();
                    }
                }
                """,
                """
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                public class ProfileTest {
                    public void open(UnifiedPage page) {
                        page.navigate(Div.class);
                    }

                    @Test
                    public void saves(UnifiedPage page) {
                        open(page);
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }

    @Test
    void anHtmlTesterMovesOutOfTheTestBenchPackage() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.flow.component.html.testbench.DivTester;
                import com.vaadin.testbench.unit.UIUnitTest;
                import org.junit.jupiter.api.Test;

                class CardTest extends UIUnitTest {
                    @Test
                    void clicks() {
                        DivTester card = test($(Div.class).single());
                        card.click();
                    }
                }
                """,
                """
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.flow.component.html.DivTester;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                class CardTest {
                    @Test
                    @RequiresTestingTool(TestingTool.BROWSERLESS)
                    void clicks(UnifiedPage page) {
                        BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
                        // TODO unified: Browserless Escape Hatch: test($(Div.class).single()), no Wrapper for Div: write one (ADR 0003)
                        DivTester card = browserless.test(browserless.$(Div.class).single());
                        card.click();
                    }
                }
                """));
    }
}
