package org.vaadin.addons.unified.rewrite;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/** The Dramafinder Migration Recipe's setup and Escape Hatch (ADR 0006), one rule per case. */
class MigrateDramafinderToUnifiedTest implements RewriteTest {

    private static final String VIEW = """
            package com.example;

            import com.vaadin.flow.component.Component;
            import com.vaadin.flow.router.Route;

            @Route("people")
            public class PeopleView extends Component {}
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        // found by name on the classpath, as the rewrite Maven and Gradle plugins find it
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.vaadin.addons.unified.rewrite").build()
                        .activateRecipes("org.vaadin.addons.unified.rewrite.MigrateDramafinderToUnified"))
                .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void springPlaywrightITBecomesUnifiedSpringBootTestAndNavigatesOnTheUnifiedPage() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

                class PeopleTest extends SpringPlaywrightIT {
                    @Test
                    void opensPeople() {
                        navigate(PeopleView.class);
                        assertAt(PeopleView.class);
                    }
                }
                """,
                """
                package com.example;

                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                @UnifiedSpringBootTest
                class PeopleTest {
                    @Test
                    void opensPeople(UnifiedPage page) {
                        page.navigate(PeopleView.class);
                        assertThat(page).isAt(PeopleView.class);
                    }
                }
                """));
    }

    @Test
    void springBootTestAndTheToolGuardGo() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
                import org.springframework.boot.test.context.SpringBootTest;
                import org.vaadin.addons.dramafinder.element.ButtonElement;
                import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

                @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
                @EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "playwright")
                class PeopleTest extends SpringPlaywrightIT {
                    @Test
                    void saves() {
                        navigate(PeopleView.class);
                        ButtonElement.getByText(page, "Save").click();
                    }
                }
                """,
                """
                package com.example;

                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;

                @UnifiedSpringBootTest
                class PeopleTest {
                    @Test
                    void saves(UnifiedPage page) {
                        page.navigate(PeopleView.class);
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }

    @Test
    void plainBaseClassBecomesUnifiedUITestAndEachTestStartsAtTheView() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;

                class PeopleTest extends AbstractBasePlaywrightIT {

                    @Override
                    public String getUrl() {
                        return "http://localhost:8080/";
                    }

                    @Override
                    public String getView() {
                        return "people";
                    }

                    @Test
                    void startsAtPeople() {
                        assertPath("people");
                    }

                    @Test
                    void navigatesItself() {
                        navigate(PeopleView.class);
                        assertQueryParameter("sort", "name");
                    }
                }
                """,
                """
                package com.example;

                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.UnifiedUITest;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                @UnifiedUITest
                class PeopleTest {

                    @Test
                    void startsAtPeople(UnifiedPage page) {
                        page.navigate("people");
                        assertThat(page).hasPath("people");
                    }

                    @Test
                    void navigatesItself(UnifiedPage page) {
                        page.navigate(PeopleView.class);
                        assertThat(page).hasQueryParameter("sort", "name");
                    }
                }
                """));
    }

    @Test
    void helpersAndBeforeEachTakeTheUnifiedPage() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.element.ButtonElement;
                import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

                class PeopleTest extends SpringPlaywrightIT {

                    @BeforeEach
                    void open() {
                        navigate(PeopleView.class);
                    }

                    @Test
                    void saves() {
                        save();
                    }

                    private void save() {
                        ButtonElement.getByText(getPage(), "Save").click();
                    }
                }
                """,
                """
                package com.example;

                import com.microsoft.playwright.Page;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.element.ButtonElement;

                @UnifiedSpringBootTest
                class PeopleTest {

                    void open(UnifiedPage page) {
                        page.navigate(PeopleView.class);
                    }

                    @Test
                    @RequiresTestingTool(TestingTool.PLAYWRIGHT)
                    void saves(UnifiedPage page) {
                        page.navigate("");
                        open(page);
                        save(page);
                    }

                    private void save(UnifiedPage page) {
                        Page playwright = page.unwrap(Page.class);
                        // TODO unified: Dramafinder Escape Hatch: ButtonElement.getByText(playwright, "Save").click()
                        ButtonElement.getByText(playwright, "Save").click();
                    }
                }
                """));
    }

    @Test
    void rawPlaywrightAndPageObjectsGetThePageFromTheEscapeHatch() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import com.microsoft.playwright.Page;

                public class PeoplePage {
                    public PeoplePage(Page page) {
                    }

                    public void save() {
                    }
                }
                """), java(
                """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

                class PeopleTest extends SpringPlaywrightIT {
                    @Test
                    void saves() {
                        navigate(PeopleView.class);
                        new PeoplePage(page).save();
                        this.page.locator("vaadin-button").click();
                    }
                }
                """,
                """
                package com.example;

                import com.microsoft.playwright.Page;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;

                @UnifiedSpringBootTest
                class PeopleTest {
                    @Test
                    @RequiresTestingTool(TestingTool.PLAYWRIGHT)
                    void saves(UnifiedPage page) {
                        Page playwright = page.unwrap(Page.class);
                        page.navigate(PeopleView.class);
                        // TODO unified: Dramafinder Escape Hatch: new PeoplePage(playwright)
                        new PeoplePage(playwright).save();
                        // TODO unified: Dramafinder Escape Hatch: playwright.locator("vaadin-button").click()
                        playwright.locator("vaadin-button").click();
                    }
                }
                """));
    }

    @Test
    void queryApiLookupsLocalsAndElementAssertionsBecomeWrappersAndUnifiedAssertions() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import static org.vaadin.addons.dramafinder.element.query.ElementQueries.find;

                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.AutoWait;
                import org.vaadin.addons.dramafinder.element.CheckboxElement;
                import org.vaadin.addons.dramafinder.element.DialogElement;
                import org.vaadin.addons.dramafinder.element.TextFieldElement;
                import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

                class PeopleTest extends SpringPlaywrightIT {
                    @Test
                    void edits() {
                        navigate(PeopleView.class);
                        TextFieldElement name = find(page, TextFieldElement.class).withLabel("Name").single();
                        name.setValue("Alan");
                        AutoWait.waitForVaadinIdle(page);
                        name.assertValue("Alan");
                        find(page, CheckboxElement.class).withLabel("Active").first().check();
                        find(page, TextFieldElement.class).get(1).assertDisabled();
                        DialogElement.getByHeaderText(page, "Edit").assertOpen();
                    }
                }
                """,
                """
                package com.example;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedCheckbox;
                import com.vaadin.testing.unified.component.UnifiedDialog;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;

                @UnifiedSpringBootTest
                class PeopleTest {
                    @Test
                    void edits(UnifiedPage page) {
                        page.navigate(PeopleView.class);
                        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
                        name.setValue("Alan");
                        assertThat(name).hasValue("Alan");
                        // TODO unified: was first(); single() fails when several components match
                        page.find(UnifiedCheckbox.class).withLabel("Active").single().check();
                        assertThat(page.find(UnifiedTextField.class).all().get(1)).isDisabled();
                        assertThat(page.find(UnifiedDialog.class).withHeaderTitle("Edit").single()).isOpen();
                    }
                }
                """));
    }

    @Test
    void aLookupWithACallTheWrapperLacksStaysBehindTheEscapeHatch() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.element.TextFieldElement;
                import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

                class PeopleTest extends SpringPlaywrightIT {
                    @Test
                    void checksThePattern() {
                        navigate(PeopleView.class);
                        TextFieldElement name = TextFieldElement.getByLabel(page, "Name");
                        name.setValue("Alan");
                        name.assertPattern("[A-Z].*");
                    }
                }
                """,
                """
                package com.example;

                import com.microsoft.playwright.Page;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.element.TextFieldElement;

                @UnifiedSpringBootTest
                class PeopleTest {
                    @Test
                    @RequiresTestingTool(TestingTool.PLAYWRIGHT)
                    void checksThePattern(UnifiedPage page) {
                        Page playwright = page.unwrap(Page.class);
                        page.navigate(PeopleView.class);
                        // TODO unified: Dramafinder Escape Hatch: TextFieldElement.getByLabel(playwright, "Name")
                        TextFieldElement name = TextFieldElement.getByLabel(playwright, "Name");
                        // TODO unified: Dramafinder Escape Hatch: name.setValue("Alan")
                        name.setValue("Alan");
                        // TODO unified: Dramafinder Escape Hatch: name.assertPattern("[A-Z].*")
                        name.assertPattern("[A-Z].*");
                    }
                }
                """));
    }

    @Test
    void aTestUsingABaseClassMemberNoRuleConvertsStaysAsItIs() {
        rewriteRun(java(VIEW), java(
                """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.vaadin.addons.dramafinder.spring.SpringPlaywrightIT;

                class AdminTest extends SpringPlaywrightIT {
                    @Test
                    void opensPeopleAsAdmin() {
                        loginAs("admin", "ADMIN");
                        navigate(PeopleView.class);
                    }
                }
                """));
    }
}
