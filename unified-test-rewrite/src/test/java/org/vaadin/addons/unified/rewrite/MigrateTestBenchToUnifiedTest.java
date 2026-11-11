package org.vaadin.addons.unified.rewrite;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/** The composite Migration Recipe, one case per rule. */
class MigrateTestBenchToUnifiedTest implements RewriteTest {

    /** The Unified API version the recipes add: this build's, installed in the local repository by the reactor. */
    private static final String UNIFIED_VERSION = System.getProperty("unified.version");

    @Override
    public void defaults(RecipeSpec spec) {
        // found by name on the classpath, as the rewrite Maven and Gradle plugins find it
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.vaadin.addons.unified.rewrite").build()
                        .activateRecipes("org.vaadin.addons.unified.rewrite.MigrateTestBenchToUnified"))
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

    @Test
    void assertEqualsOnValueOrTextBecomesARetryingAssertion() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.button.testbench.ButtonElement;
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void showsTheProfile() {
                        Assertions.assertEquals("Alice", $(TextFieldElement.class).withLabel("Name").single().getValue());
                        Assertions.assertEquals("Save", $(ButtonElement.class).withText("Save").single().getText());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class ProfileTest {

                    @Test
                    void showsTheProfile(UnifiedPage page) {
                        assertThat(page.find(UnifiedTextField.class).withLabel("Name").single()).hasValue("Alice");
                        assertThat(page.find(UnifiedButton.class).withText("Save").single()).hasText("Save");
                    }
                }
                """));
    }

    @Test
    void buildSwapsTestBenchForTheUnifiedBomAndAdapters() {
        rewriteRun(pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.vaadin</groupId>
                                <artifactId>vaadin-bom</artifactId>
                                <version>25.3.5</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.vaadin</groupId>
                            <artifactId>vaadin-testbench-core-junit6</artifactId>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.vaadin</groupId>
                                <artifactId>vaadin-bom</artifactId>
                                <version>25.3.5</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                            <dependency>
                                <groupId>org.vaadin.addons</groupId>
                                <artifactId>unified-test-bom</artifactId>
                                <version>%s</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.vaadin.addons</groupId>
                            <artifactId>unified-test-browserless</artifactId>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.vaadin.addons</groupId>
                            <artifactId>unified-test-selenium</artifactId>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.vaadin.addons</groupId>
                            <artifactId>unified-test-spring</artifactId>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """.formatted(UNIFIED_VERSION)));
    }

    @Test
    void addPlaywrightAlsoAddsThePlaywrightAdapter() {
        rewriteRun(
                spec -> spec.recipe(new MigrateTestBenchToUnified(true)),
                pomXml(
                        """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>com.example</groupId>
                            <artifactId>app</artifactId>
                            <version>1.0</version>
                            <dependencies>
                                <dependency>
                                    <groupId>com.vaadin</groupId>
                                    <artifactId>vaadin-testbench-core-junit6</artifactId>
                                    <version>25.3.2</version>
                                    <scope>test</scope>
                                </dependency>
                            </dependencies>
                        </project>
                        """,
                        """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>com.example</groupId>
                            <artifactId>app</artifactId>
                            <version>1.0</version>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.vaadin.addons</groupId>
                                        <artifactId>unified-test-bom</artifactId>
                                        <version>%s</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.vaadin.addons</groupId>
                                    <artifactId>unified-test-browserless</artifactId>
                                    <scope>test</scope>
                                </dependency>
                                <dependency>
                                    <groupId>org.vaadin.addons</groupId>
                                    <artifactId>unified-test-playwright</artifactId>
                                    <scope>test</scope>
                                </dependency>
                                <dependency>
                                    <groupId>org.vaadin.addons</groupId>
                                    <artifactId>unified-test-selenium</artifactId>
                                    <scope>test</scope>
                                </dependency>
                                <dependency>
                                    <groupId>org.vaadin.addons</groupId>
                                    <artifactId>unified-test-spring</artifactId>
                                    <scope>test</scope>
                                </dependency>
                            </dependencies>
                        </project>
                        """.formatted(UNIFIED_VERSION)));
    }

    @Test
    void aTestGatedOnTheTestingToolSettingRunsOnEveryTestingTool() {
        rewriteRun(java(
                """
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

                @EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void opensProfile() {
                        getDriver().get("http://localhost:8080/profile");
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import org.junit.jupiter.api.Test;

                class ProfileTest {

                    @Test
                    void opensProfile(UnifiedPage page) {
                        page.navigate("profile");
                    }
                }
                """));
    }

    @Test
    void leftoverTestBenchCallsRunThroughTheEscapeHatchOnSeleniumOnly() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.grid.testbench.GridElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class PeopleTest extends BrowserTestBase {

                    @BrowserTest
                    void countsPeople() {
                        getDriver().get("http://localhost:8080/people");
                        int rows = $(GridElement.class).first().getRowCount();
                        executeScript("window.scrollTo(0, 0)");
                    }
                }
                """,
                """
                import com.vaadin.flow.component.grid.testbench.GridElement;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
                import org.junit.jupiter.api.Test;

                class PeopleTest {

                    @Test
                    @RequiresTestingTool(TestingTool.SELENIUM)
                    void countsPeople(UnifiedPage page) {
                        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
                        page.navigate("people");
                        // TODO unified: TestBench Escape Hatch: $(GridElement.class)
                        int rows = testBench.$(GridElement.class).first().getRowCount();
                        // TODO unified: TestBench Escape Hatch: executeScript("window.scrollTo(0, 0)")
                        testBench.executeScript("window.scrollTo(0, 0)");
                    }
                }
                """));
    }

    @Test
    void aLookupStaysTestBenchWhenTheWrapperLacksACallUsedOnIt() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void typesTheName() {
                        $(TextFieldElement.class).withLabel("Name").single().sendKeys("Alice");
                        $(TextFieldElement.class).withLabel("Email").single().setValue("alice@example.com");
                    }
                }
                """,
                """
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
                import org.junit.jupiter.api.Test;

                class ProfileTest {

                    @Test
                    @RequiresTestingTool(TestingTool.SELENIUM)
                    void typesTheName(UnifiedPage page) {
                        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
                        // TODO unified: TestBench Escape Hatch: $(TextFieldElement.class)
                        testBench.$(TextFieldElement.class).withLabel("Name").single().sendKeys("Alice");
                        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("alice@example.com");
                    }
                }
                """));
    }

    @Test
    void anAssertionStaysJUnitWhenItsLookupStaysTestBench() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void showsTheNickname() {
                        Assertions.assertEquals("Ali", $(TextFieldElement.class).withAttribute("name", "nick").single().getValue());
                    }
                }
                """,
                """
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class ProfileTest {

                    @Test
                    @RequiresTestingTool(TestingTool.SELENIUM)
                    void showsTheNickname(UnifiedPage page) {
                        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
                        // TODO unified: TestBench Escape Hatch: $(TextFieldElement.class)
                        Assertions.assertEquals("Ali", testBench.$(TextFieldElement.class).withAttribute("name", "nick").single().getValue());
                    }
                }
                """));
    }
}
