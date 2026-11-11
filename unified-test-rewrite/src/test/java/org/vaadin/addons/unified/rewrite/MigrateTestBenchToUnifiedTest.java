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
                import com.vaadin.flow.component.upload.testbench.UploadElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class PeopleTest extends BrowserTestBase {

                    @BrowserTest
                    void countsPeople() {
                        getDriver().get("http://localhost:8080/people");
                        String text = $(UploadElement.class).first().getText();
                        executeScript("window.scrollTo(0, 0)");
                    }
                }
                """,
                """
                import com.vaadin.flow.component.upload.testbench.UploadElement;
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
                        // TODO unified: TestBench Escape Hatch: $(UploadElement.class)
                        String text = testBench.$(UploadElement.class).first().getText();
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

    @Test
    void aLocalHoldingAConvertedLookupTakesTheWrapperType() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void savesTheName() {
                        TextFieldElement name = $(TextFieldElement.class).withLabel("Name").single();
                        name.setValue("Alice");
                        Assertions.assertEquals("Alice", name.getValue());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class ProfileTest {

                    @Test
                    void savesTheName(UnifiedPage page) {
                        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
                        name.setValue("Alice");
                        assertThat(name).hasValue("Alice");
                    }
                }
                """));
    }

    @Test
    void firstBecomesSingleWithAMarker() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.button.testbench.ButtonElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void saves() {
                        $(ButtonElement.class).withText("Save").first().click();
                        Assertions.assertEquals("Archive", $(ButtonElement.class).withText("Archive").waitForFirst().getText());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class ProfileTest {

                    @Test
                    void saves(UnifiedPage page) {
                        // TODO unified: was first(); single() fails when several components match
                        page.find(UnifiedButton.class).withText("Save").single().click();
                        // TODO unified: was waitForFirst(); single() fails when several components match
                        assertThat(page.find(UnifiedButton.class).withText("Archive").single()).hasText("Archive");
                    }
                }
                """));
    }

    @Test
    void beforeEachAndHelpersUsingTestBenchGetThePage() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.button.testbench.ButtonElement;
                import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.BeforeEach;

                class ProfileTest extends BrowserTestBase {

                    @BeforeEach
                    void open() {
                        getDriver().get("http://localhost:8080/profile");
                    }

                    @BrowserTest
                    void savesTheName() {
                        fillName("Alice");
                        $(ButtonElement.class).withText("Save").single().click();
                    }

                    private void fillName(String name) {
                        $(TextFieldElement.class).withLabel("Name").single().setValue(name);
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedButton;
                import com.vaadin.testing.unified.component.UnifiedTextField;
                import org.junit.jupiter.api.Test;

                class ProfileTest {

                    void open(UnifiedPage page) {
                        page.navigate("profile");
                    }

                    @Test
                    void savesTheName(UnifiedPage page) {
                        open(page);
                        fillName(page, "Alice");
                        page.find(UnifiedButton.class).withText("Save").single().click();
                    }

                    private void fillName(UnifiedPage page, String name) {
                        page.find(UnifiedTextField.class).withLabel("Name").single().setValue(name);
                    }
                }
                """));
    }

    @Test
    void aTestCallingAHelperThatEscapesRequiresSelenium() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.upload.testbench.UploadElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class PeopleTest extends BrowserTestBase {

                    @BrowserTest
                    void countsPeople() {
                        uploadText();
                    }

                    private String uploadText() {
                        return $(UploadElement.class).first().getText();
                    }
                }
                """,
                """
                import com.vaadin.flow.component.upload.testbench.UploadElement;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
                import org.junit.jupiter.api.Test;

                class PeopleTest {

                    @Test
                    @RequiresTestingTool(TestingTool.SELENIUM)
                    void countsPeople(UnifiedPage page) {
                        uploadText(page);
                    }

                    private String uploadText(UnifiedPage page) {
                        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
                        // TODO unified: TestBench Escape Hatch: $(UploadElement.class)
                        return testBench.$(UploadElement.class).first().getText();
                    }
                }
                """));
    }

    @Test
    void notificationLookupsFindTheUnifiedNotification() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.notification.testbench.NotificationElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void confirmsTheSave() {
                        Assertions.assertEquals("Saved", $(NotificationElement.class).waitForFirst().getText());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedNotification;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class ProfileTest {

                    @Test
                    void confirmsTheSave(UnifiedPage page) {
                        // TODO unified: was waitForFirst(); single() fails when several components match
                        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved");
                    }
                }
                """));
    }

    @Test
    void gridLookupsFindTheUnifiedGridAndCellsByIndexReadThroughTheRow() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.grid.testbench.GridElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class PeopleTest extends BrowserTestBase {

                    @BrowserTest
                    void listsPeople() {
                        GridElement people = $(GridElement.class).waitForFirst();
                        Assertions.assertEquals("Alan Turing", people.getCell(0, 0).getText());
                        Assertions.assertEquals(3, people.getRowCount());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedGrid;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                class PeopleTest {

                    @Test
                    void listsPeople(UnifiedPage page) {
                        // TODO unified: was waitForFirst(); single() fails when several components match
                        UnifiedGrid people = page.find(UnifiedGrid.class).single();
                        Assertions.assertEquals("Alan Turing", people.row(0).getCellText(0));
                        Assertions.assertEquals(3, people.getRowCount());
                    }
                }
                """));
    }
}
