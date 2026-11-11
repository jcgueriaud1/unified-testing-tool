package com.vaadin.testing.unified.rewrite;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/** The composite Migration Recipe, one case per rule. */
class MigrateTestBenchToUnifiedTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.vaadin.addons.unified.rewrite.MigrateTestBenchToUnified")
                .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void browserTestBecomesUnifiedSpringBootTestOnAUnifiedPage() {
        rewriteRun(java(
                """
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.springframework.boot.test.context.SpringBootTest;
                import org.springframework.boot.test.web.server.LocalServerPort;

                @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
                class ProfileTest extends BrowserTestBase {

                    @LocalServerPort
                    int port;

                    @BrowserTest
                    void opensProfile() {
                        getDriver().get("http://localhost:" + port + "/profile");
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                import org.junit.jupiter.api.Test;

                @UnifiedSpringBootTest
                class ProfileTest {

                    @Test
                    void opensProfile(UnifiedPage page) {
                        page.navigate("profile");
                    }
                }
                """));
    }

    @Test
    void navigatesToTheViewClassWhenOneViewHasTheRoute() {
        rewriteRun(
                java(
                        """
                        package demo;

                        import com.vaadin.flow.component.Component;
                        import com.vaadin.flow.router.Route;

                        @Route("profile")
                        public class ProfileView extends Component {
                        }
                        """),
                java(
                        """
                        package demo;

                        import com.vaadin.testbench.BrowserTest;
                        import com.vaadin.testbench.BrowserTestBase;

                        class ProfileTest extends BrowserTestBase {

                            @BrowserTest
                            void opensProfile() {
                                getDriver().get("http://localhost:8080/profile");
                            }
                        }
                        """,
                        """
                        package demo;

                        import com.vaadin.testing.unified.UnifiedPage;
                        import org.junit.jupiter.api.Test;

                        class ProfileTest {

                            @Test
                            void opensProfile(UnifiedPage page) {
                                page.navigate(ProfileView.class);
                            }
                        }
                        """));
    }

    @Test
    void textFieldLookupsFindTheUnifiedTextField() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void fillsTheName() {
                        $(TextFieldElement.class).withLabel("Name").single().setValue("Alice");
                        $(TextFieldElement.class).withId("email").single().setValue("alice@example.com");
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                class ProfileTest {

                    @Test
                    void fillsTheName(UnifiedPage page) {
                        page.find(UnifiedTextField.class).withLabel("Name").single().setValue("Alice");
                        page.find(UnifiedTextField.class).withId("email").single().setValue("alice@example.com");
                    }
                }
                """));
    }

    @Test
    void buttonLookupsFindTheUnifiedButton() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.button.testbench.ButtonElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void saves() {
                        $(ButtonElement.class).withText("Save").single().click();
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import org.junit.jupiter.api.Test;

                class ProfileTest {

                    @Test
                    void saves(UnifiedPage page) {
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }
                }
                """));
    }
}
