package org.vaadin.addons.unified.rewrite;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/** The Browserless Migration Recipe's test code changes (ADR 0005), one rule per case. */
class MigrateBrowserlessToUnifiedTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // found by name on the classpath, as the rewrite Maven and Gradle plugins find it
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.vaadin.addons.unified.rewrite").build()
                        .activateRecipes("org.vaadin.addons.unified.rewrite.MigrateBrowserlessToUnified"))
                .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void springBrowserlessTestBecomesUnifiedSpringBootTestOnAUnifiedPage() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.SpringBrowserlessTest;
                import com.vaadin.flow.component.button.Button;
                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.context.SpringBootTest;

                @SpringBootTest
                class ProfileTest extends SpringBrowserlessTest {
                    @Test
                    void saves() {
                        test($(Button.class).withText("Save").single()).click();
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;

                @UnifiedSpringBootTest
                class ProfileTest {
                    @Test
                    void saves(UnifiedPage page) {
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }

    @Test
    void plainBrowserlessTestBecomesUnifiedUITestAndKeepsItsViewPackages() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.browserless.ViewPackages;
                import com.vaadin.flow.component.button.Button;
                import org.junit.jupiter.api.Test;

                @ViewPackages(packages = "com.example")
                class ProfileTest extends BrowserlessTest {
                    @Test
                    void saves() {
                        test($(Button.class).withText("Save").single()).click();
                    }
                }
                """,
                """
                import com.vaadin.browserless.ViewPackages;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import org.junit.jupiter.api.Test;

                @ViewPackages(packages = "com.example")
                @UnifiedUITest
                class ProfileTest {
                    @Test
                    void saves(UnifiedPage page) {
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }

    @Test
    void helpersAndBeforeEachGetThePage() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.flow.component.textfield.TextField;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;

                class ProfileTest extends BrowserlessTest {
                    @BeforeEach
                    void open() {
                        navigate(Div.class);
                    }

                    @Test
                    void types() {
                        typeName("Alice");
                    }

                    private void typeName(String name) {
                        test($(TextField.class).withLabel("Name").single()).setValue(name);
                    }
                }
                """,
                """
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                class ProfileTest {
                    void open(UnifiedPage page) {
                        page.navigate(Div.class);
                    }

                    @Test
                    void types(UnifiedPage page) {
                        open(page);
                        typeName(page, "Alice");
                    }

                    private void typeName(UnifiedPage page, String name) {
                        page.find(UnifiedTextField.class).withLabel("Name").single().setValue(name);
                    }
                }
                """));
    }

    @Test
    void aLocalTakesTheWrapperType() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.textfield.TextField;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class FieldTest extends BrowserlessTest {
                    @Test
                    void types() {
                        TextField name = $(TextField.class).withCaption("Name").single();
                        test(name).setValue("Alice");
                        Assertions.assertEquals("Alice", name.getValue());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                @UnifiedUITest
                class FieldTest {
                    @Test
                    void types(UnifiedPage page) {
                        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
                        name.setValue("Alice");
                        assertThat(name).hasValue("Alice");
                    }
                }
                """));
    }

    @Test
    void checkboxCallsAndChecksTakeTheirWrapperNames() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.checkbox.Checkbox;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class TermsTest extends BrowserlessTest {
                    @Test
                    void accepts() {
                        test($(Checkbox.class).withLabel("Terms").single()).check();
                        Assertions.assertTrue($(Checkbox.class).withLabel("Terms").single().getValue());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedCheckbox;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                @UnifiedUITest
                class TermsTest {
                    @Test
                    void accepts(UnifiedPage page) {
                        page.find(UnifiedCheckbox.class).withLabel("Terms").single().check();
                        assertThat(page.find(UnifiedCheckbox.class).withLabel("Terms").single()).isChecked();
                    }
                }
                """));
    }

    @Test
    void buttonCaptionBecomesTextAndOtherTerminalsBecomeSingle() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.button.Button;
                import com.vaadin.flow.component.textfield.TextField;
                import org.junit.jupiter.api.Test;

                class ProfileTest extends BrowserlessTest {
                    @Test
                    void saves() {
                        test($(TextField.class).last()).setValue("x");
                        test($(TextField.class).atIndex(2)).clear();
                        test($(Button.class).withCaption("Save").single()).click();
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                class ProfileTest {
                    @Test
                    void saves(UnifiedPage page) {
                        // TODO unified: was last(); single() fails when several components match
                        page.find(UnifiedTextField.class).single().setValue("x");
                        // TODO unified: was atIndex(2); single() fails when several components match
                        page.find(UnifiedTextField.class).single().clear();
                        // TODO unified: was withCaption("Save"); use withAccessibleName(..) for a button without text
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }

    @Test
    void callsWithAUnifiedMeaningAreConverted() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.Key;
                import com.vaadin.flow.component.KeyModifier;
                import com.vaadin.flow.component.UI;
                import com.vaadin.flow.component.html.Div;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class ViewTest extends BrowserlessTest {
                    @Test
                    void opens() {
                        UI.getCurrent().navigate(Div.class);
                        roundTrip();
                        fireShortcut(Key.KEY_S, KeyModifier.CONTROL);

                        Assertions.assertTrue(getCurrentView() instanceof Div);
                        Assertions.assertInstanceOf(Div.class, getCurrentView());
                    }
                }
                """,
                """
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                @UnifiedUITest
                class ViewTest {
                    @Test
                    void opens(UnifiedPage page) {
                        page.navigate(Div.class);
                        // TODO unified: was fireShortcut(..); pressKey also reaches the focused component and an open dialog
                        page.pressKey("Control+s");

                        assertThat(page).isAt(Div.class);
                        assertThat(page).isAt(Div.class);
                    }
                }
                """));
    }

    @Test
    void leftoversRunThroughTheEscapeHatchAndRequireBrowserless() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.grid.Grid;
                import com.vaadin.flow.component.textfield.TextField;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class ProfileTest extends BrowserlessTest {
                    TextField name;

                    @Test
                    void readsTheView() {
                        test(name).setValue("Alice");
                        test($(TextField.class).withCondition(f -> f.isRequired()).single()).setValue("x");
                        Assertions.assertEquals(3, test($(Grid.class).single()).size());
                    }
                }
                """,
                """
                import com.vaadin.flow.component.grid.Grid;
                import com.vaadin.flow.component.textfield.TextField;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                class ProfileTest {
                    TextField name;

                    @Test
                    @RequiresTestingTool(TestingTool.BROWSERLESS)
                    void readsTheView(UnifiedPage page) {
                        BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
                        // TODO unified: Component Reference: test(name).setValue("Alice"): replace it with a locator that finds the same component
                        browserless.test(name).setValue("Alice");
                        // TODO unified: filter runs Java on the component: test($(TextField.class).withCondition(f -> f.isRequired()).single()).setValue("x"): replace withCondition(..) with a locator
                        browserless.test(browserless.$(TextField.class).withCondition(f -> f.isRequired()).single()).setValue("x");
                        // TODO unified: Browserless Escape Hatch: test($(Grid.class).single()).size(), no Wrapper for Grid: write one (ADR 0003)
                        Assertions.assertEquals(3, browserless.test(browserless.$(Grid.class).single()).size());
                    }
                }
                """));
    }

    @Test
    void aTestCallingAHelperThatEscapesRequiresBrowserlessToo() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.grid.Grid;
                import org.junit.jupiter.api.Test;

                class PeopleTest extends BrowserlessTest {
                    @Test
                    void counts() {
                        count();
                    }

                    private int count() {
                        return test($(Grid.class).single()).size();
                    }
                }
                """,
                """
                import com.vaadin.flow.component.grid.Grid;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                class PeopleTest {
                    @Test
                    @RequiresTestingTool(TestingTool.BROWSERLESS)
                    void counts(UnifiedPage page) {
                        count(page);
                    }

                    private int count(UnifiedPage page) {
                        BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
                        // TODO unified: Browserless Escape Hatch: test($(Grid.class).single()).size(), no Wrapper for Grid: write one (ADR 0003)
                        return browserless.test(browserless.$(Grid.class).single()).size();
                    }
                }
                """));
    }

    @Test
    void aLookupWithACallItsWrapperLacksStaysBehindTheEscapeHatch() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessTest;
                import com.vaadin.flow.component.textfield.TextField;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class ProfileTest extends BrowserlessTest {
                    @Test
                    void reads() {
                        Assertions.assertEquals(20, $(TextField.class).withLabel("Name").single().getMaxLength());
                    }
                }
                """,
                """
                import com.vaadin.flow.component.textfield.TextField;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.browserless.BrowserlessEscapeHatch;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                class ProfileTest {
                    @Test
                    @RequiresTestingTool(TestingTool.BROWSERLESS)
                    void reads(UnifiedPage page) {
                        BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
                        // TODO unified: Browserless Escape Hatch: $(TextField.class).withLabel("Name").single().getMaxLength()
                        Assertions.assertEquals(20, browserless.$(TextField.class).withLabel("Name").single().getMaxLength());
                    }
                }
                """));
    }

    @Test
    void aTransactionalTestRequiresBrowserless() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.SpringBrowserlessTest;
                import com.vaadin.flow.component.button.Button;
                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.context.SpringBootTest;
                import org.springframework.transaction.annotation.Transactional;

                @SpringBootTest
                class ProfileTest extends SpringBrowserlessTest {
                    @Test
                    @Transactional
                    void saves() {
                        test($(Button.class).withText("Save").single()).click();
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;
                import org.springframework.transaction.annotation.Transactional;

                @UnifiedSpringBootTest
                class ProfileTest {
                    // TODO unified: Browserless-only pattern: @Transactional rolls back what the views write only on Browserless; commit test data and clean up to run everywhere (spec 02)
                    @Test
                    @Transactional
                    @RequiresTestingTool(TestingTool.BROWSERLESS)
                    void saves(UnifiedPage page) {
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }

    @Test
    void aNarrowSpringContextRequiresBrowserlessAndKeepsTheSpringExtension() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.SpringBrowserlessTest;
                import com.vaadin.flow.component.button.Button;
                import org.junit.jupiter.api.Test;
                import org.springframework.test.context.ContextConfiguration;

                @ContextConfiguration(classes = Object.class)
                class ProfileTest extends SpringBrowserlessTest {
                    @Test
                    void saves() {
                        test($(Button.class).withText("Save").single()).click();
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.extension.ExtendWith;
                import org.springframework.test.context.ContextConfiguration;
                import org.springframework.test.context.junit.jupiter.SpringExtension;

                // TODO unified: Browserless-only pattern: a Spring context without @SpringBootTest has no web server; use @UnifiedSpringBootTest with @MockitoBean to run everywhere (spec 02)
                @ContextConfiguration(classes = Object.class)
                @ExtendWith(SpringExtension.class)
                @RequiresTestingTool(TestingTool.BROWSERLESS)
                @UnifiedUITest
                class ProfileTest {
                    @Test
                    void saves(UnifiedPage page) {
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }

    @Test
    void aClassPassingItselfToAPageObjectIsMarkedAndLeftAsItIs() {
        rewriteRun(
                java(
                        """
                        import com.vaadin.browserless.BrowserlessTest;

                        class LoginPage {
                            LoginPage(BrowserlessTest test) {
                            }
                        }
                        """),
                java(
                        """
                        import com.vaadin.browserless.BrowserlessTest;
                        import org.junit.jupiter.api.Test;

                        class LoginTest extends BrowserlessTest {
                            @Test
                            void logsIn() {
                                new LoginPage(this);
                            }
                        }
                        """,
                        """
                        import com.vaadin.browserless.BrowserlessTest;
                        import org.junit.jupiter.api.Test;

                        // TODO unified: not migrated: this class passes itself (this) to a page object or helper; make them take a UnifiedPage, then run the migration again
                        class LoginTest extends BrowserlessTest {
                            @Test
                            void logsIn() {
                                new LoginPage(this);
                            }
                        }
                        """));
    }

    @Test
    void addAgentSkillWritesTheMigrationMarkersSkill() throws Exception {
        String skill = new String(getClass().getResourceAsStream(
                "/META-INF/unified/skills/resolve-migration-markers/SKILL.md").readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        rewriteRun(
                spec -> spec.recipe(new MigrateBrowserlessToUnified(null, null, true)),
                org.openrewrite.test.SourceSpecs.text(null, skill,
                        source -> source.path(".claude/skills/resolve-migration-markers/SKILL.md")));
    }

    @Test
    void anExtensionTestBecomesAUnifiedUITestWithItsViewPackages() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessExtension;
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.flow.component.textfield.TextField;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.extension.RegisterExtension;

                class ProfileTest {
                    @RegisterExtension
                    BrowserlessExtension browserless = new BrowserlessExtension().withViewPackages(Div.class);

                    @Test
                    void types() {
                        browserless.navigate(Div.class);
                        browserless.findTextField().withLabel("Name").setValue("Alice");
                        browserless.test(browserless.find(TextField.class).withLabel("Email").single()).setValue("a@b");
                    }
                }
                """,
                """
                import com.vaadin.browserless.ViewPackages;
                import com.vaadin.flow.component.html.Div;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                @UnifiedUITest
                @ViewPackages(classes = {Div.class})
                class ProfileTest {

                    @Test
                    void types(UnifiedPage page) {
                        page.navigate(Div.class);
                        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");
                        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("a@b");
                    }
                }
                """));
    }

    @Test
    void aContextApiTestLosesItsWiringAndAssertsOnTheLocatorsLookup() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessApplicationContext;
                import com.vaadin.browserless.BrowserlessUIContext;
                import org.junit.jupiter.api.AfterEach;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;

                class ProfileTest {
                    BrowserlessUIContext window;

                    @BeforeEach
                    void open() {
                        window = BrowserlessApplicationContext.create("com.example").newUser().newWindow();
                    }

                    @AfterEach
                    void close() {
                        window.getUser().close();
                    }

                    @Test
                    void reads() {
                        window.findButton().withText("Save").click();
                        Assertions.assertEquals("Saved", window.findNotification().getText());
                    }
                }
                """,
                """
                import com.vaadin.browserless.ViewPackages;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.component.UnifiedNotification;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                @UnifiedUITest
                @ViewPackages(packages = {"com.example"})
                class ProfileTest {

                    @Test
                    void reads(UnifiedPage page) {
                        page.find(UnifiedButton.class).withText("Save").single().click();
                        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved");
                    }
                }
                """));
    }

    @Test
    void aContextApiTestUsingItsApplicationIsMarkedAndLeftAsItIs() {
        rewriteRun(java(
                """
                import com.vaadin.browserless.BrowserlessApplicationContext;
                import com.vaadin.browserless.BrowserlessUIContext;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;

                class TwoUsersTest {
                    BrowserlessApplicationContext app;
                    BrowserlessUIContext window;

                    @BeforeEach
                    void open() {
                        app = BrowserlessApplicationContext.create("com.example");
                        window = app.newUser().newWindow();
                    }

                    @Test
                    void seesTheOtherUser() {
                        BrowserlessUIContext other = app.newUser().newWindow();
                    }
                }
                """,
                """
                import com.vaadin.browserless.BrowserlessApplicationContext;
                import com.vaadin.browserless.BrowserlessUIContext;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;

                // TODO unified: not migrated: this test uses the Browserless application or user context beyond opening a window; use page.openWindow() or page.openAsUser(..) instead, then run the migration again
                class TwoUsersTest {
                    BrowserlessApplicationContext app;
                    BrowserlessUIContext window;

                    @BeforeEach
                    void open() {
                        app = BrowserlessApplicationContext.create("com.example");
                        window = app.newUser().newWindow();
                    }

                    @Test
                    void seesTheOtherUser() {
                        BrowserlessUIContext other = app.newUser().newWindow();
                    }
                }
                """));
    }

    @Test
    void aClassThatIsNotABrowserlessTestIsLeftAlone() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.textfield.TextField;
                import org.junit.jupiter.api.Test;

                class FieldTest {
                    @Test
                    void sets() {
                        new TextField().setValue("x");
                    }
                }
                """));
    }
}
