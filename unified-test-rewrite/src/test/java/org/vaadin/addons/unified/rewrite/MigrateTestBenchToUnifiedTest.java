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
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-dependencies</artifactId>
                                <version>4.1.1</version>
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
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-dependencies</artifactId>
                                <version>4.1.1</version>
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
                                        <groupId>org.springframework.boot</groupId>
                                        <artifactId>spring-boot-dependencies</artifactId>
                                        <version>4.1.1</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
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
                                        <groupId>com.vaadin</groupId>
                                        <artifactId>vaadin-bom</artifactId>
                                        <version>25.3.5</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                    <dependency>
                                        <groupId>org.springframework.boot</groupId>
                                        <artifactId>spring-boot-dependencies</artifactId>
                                        <version>4.1.1</version>
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

    @Test
    void aCellFoundByTextBecomesItsRow() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.grid.testbench.GridElement;
                import com.vaadin.flow.component.grid.testbench.GridTHTDElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class PeopleTest extends BrowserTestBase {

                    @BrowserTest
                    void opensAlan() {
                        GridElement people = $(GridElement.class).single();
                        GridTHTDElement alan = people.getCell("Alan Turing");
                        int row = alan.getRow();
                        alan.click();
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedGrid;
                import org.junit.jupiter.api.Test;

                class PeopleTest {

                    @Test
                    void opensAlan(UnifiedPage page) {
                        UnifiedGrid people = page.find(UnifiedGrid.class).single();
                        // TODO unified: was getCell("Alan Turing"); single() fails when several rows match
                        UnifiedGrid.UnifiedGridRow alan = people.rows().withCellText("Alan Turing").single();
                        int row = alan.getIndex();
                        alan.click();
                    }
                }
                """));
    }

    @Test
    void checkboxLookupsAndCheckedAssertions() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class PreferencesTest extends BrowserTestBase {

                    @BrowserTest
                    void togglesNewsletter() {
                        CheckboxElement newsletter = $(CheckboxElement.class).withLabel("Newsletter").single();
                        newsletter.setChecked(true);
                        Assertions.assertTrue(newsletter.isChecked());
                        Assertions.assertFalse($(CheckboxElement.class).withLabel("Dark mode").single().isChecked());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedCheckbox;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class PreferencesTest {

                    @Test
                    void togglesNewsletter(UnifiedPage page) {
                        UnifiedCheckbox newsletter = page.find(UnifiedCheckbox.class).withLabel("Newsletter").single();
                        newsletter.setChecked(true);
                        assertThat(newsletter).isChecked();
                        assertThat(page.find(UnifiedCheckbox.class).withLabel("Dark mode").single()).isNotChecked();
                    }
                }
                """));
    }

    @Test
    void comboBoxCallsTakeTheirUnifiedNames() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.combobox.testbench.ComboBoxElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class OrderTest extends BrowserTestBase {

                    @BrowserTest
                    void picksAProduct() {
                        ComboBoxElement product = $(ComboBoxElement.class).withLabel("Product").single();
                        product.selectByText("Tea");
                        Assertions.assertEquals("Tea", product.getSelectedText());
                        Assertions.assertEquals(3, product.getOptions().size());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedComboBox;
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class OrderTest {

                    @Test
                    void picksAProduct(UnifiedPage page) {
                        UnifiedComboBox product = page.find(UnifiedComboBox.class).withLabel("Product").single();
                        product.selectItem("Tea");
                        assertThat(product).hasSelectedItem("Tea");
                        Assertions.assertEquals(3, product.getItems().size());
                    }
                }
                """));
    }

    @Test
    void radioButtonGroupCallsTakeTheirUnifiedNames() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.radiobutton.testbench.RadioButtonGroupElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class OrderTest extends BrowserTestBase {

                    @BrowserTest
                    void picksDelivery() {
                        $(RadioButtonGroupElement.class).withLabel("Delivery").single().selectByText("Express");
                        Assertions.assertEquals("Express", $(RadioButtonGroupElement.class).withLabel("Delivery").single().getSelectedText());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedRadioButtonGroup;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class OrderTest {

                    @Test
                    void picksDelivery(UnifiedPage page) {
                        page.find(UnifiedRadioButtonGroup.class).withLabel("Delivery").single().selectItem("Express");
                        assertThat(page.find(UnifiedRadioButtonGroup.class).withLabel("Delivery").single()).hasSelectedItem("Express");
                    }
                }
                """));
    }

    @Test
    void multiSelectComboBoxCallsTakeTheirUnifiedNames() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.combobox.testbench.MultiSelectComboBoxElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import java.util.List;
                import org.junit.jupiter.api.Assertions;

                class OrderTest extends BrowserTestBase {

                    @BrowserTest
                    void picksExtras() {
                        MultiSelectComboBoxElement extras = $(MultiSelectComboBoxElement.class).withLabel("Extras").single();
                        extras.selectByText("Milk");
                        extras.selectByText("Sugar");
                        extras.deselectByText("Milk");
                        Assertions.assertEquals(List.of("Sugar"), extras.getSelectedTexts());
                        Assertions.assertEquals(List.of("Milk", "Sugar", "Honey"), extras.getOptions());
                        extras.deselectAll();
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedMultiSelectComboBox;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class OrderTest {

                    @Test
                    void picksExtras(UnifiedPage page) {
                        UnifiedMultiSelectComboBox extras = page.find(UnifiedMultiSelectComboBox.class).withLabel("Extras").single();
                        extras.selectItems("Milk");
                        extras.selectItems("Sugar");
                        extras.deselectItems("Milk");
                        assertThat(extras).hasSelectedItems("Sugar");
                        assertThat(extras).hasItems("Milk", "Sugar", "Honey");
                        extras.clear();
                    }
                }
                """));
    }

    @Test
    void datePickerCallsTakeTheirUnifiedNames() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.datepicker.testbench.DatePickerElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import java.time.LocalDate;
                import org.junit.jupiter.api.Assertions;

                class PreferencesTest extends BrowserTestBase {

                    @BrowserTest
                    void setsTheBirthday() {
                        DatePickerElement birthday = $(DatePickerElement.class).withLabel("Birthday").single();
                        birthday.setDate(LocalDate.of(1990, 5, 17));
                        Assertions.assertEquals(LocalDate.of(1990, 5, 17), birthday.getDate());
                        birthday.setInputValue("1/2/2000");
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedDatePicker;
                import org.junit.jupiter.api.Test;

                import java.time.LocalDate;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class PreferencesTest {

                    @Test
                    void setsTheBirthday(UnifiedPage page) {
                        UnifiedDatePicker birthday = page.find(UnifiedDatePicker.class).withLabel("Birthday").single();
                        birthday.setValue(LocalDate.of(1990, 5, 17));
                        assertThat(birthday).hasValue(LocalDate.of(1990, 5, 17));
                        birthday.typeText("1/2/2000");
                    }
                }
                """));
    }

    @Test
    void numberFieldStringLiteralsBecomeNumbers() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.textfield.testbench.NumberFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;
                import org.junit.jupiter.api.Assertions;

                class PreferencesTest extends BrowserTestBase {

                    @BrowserTest
                    void setsTheAge() {
                        NumberFieldElement age = $(NumberFieldElement.class).withLabel("Age").single();
                        age.setValue("42");
                        Assertions.assertEquals("42", age.getValue());
                    }
                }
                """,
                """
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.component.UnifiedNumberField;
                import org.junit.jupiter.api.Test;

                import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

                class PreferencesTest {

                    @Test
                    void setsTheAge(UnifiedPage page) {
                        UnifiedNumberField age = page.find(UnifiedNumberField.class).withLabel("Age").single();
                        age.setValue(42.0);
                        assertThat(age).hasValue(42.0);
                    }
                }
                """));
    }

    @Test
    void aNumberFieldValueThatIsNotALiteralStaysTestBench() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.textfield.testbench.NumberFieldElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class PreferencesTest extends BrowserTestBase {

                    @BrowserTest
                    void setsTheAge() {
                        String age = "42";
                        $(NumberFieldElement.class).withLabel("Age").single().setValue(age);
                    }
                }
                """,
                """
                import com.vaadin.flow.component.textfield.testbench.NumberFieldElement;
                import com.vaadin.testing.unified.RequiresTestingTool;
                import com.vaadin.testing.unified.TestingTool;
                import com.vaadin.testing.unified.UnifiedPage;
                import com.vaadin.testing.unified.selenium.TestBenchEscapeHatch;
                import org.junit.jupiter.api.Test;

                class PreferencesTest {

                    @Test
                    @RequiresTestingTool(TestingTool.SELENIUM)
                    void setsTheAge(UnifiedPage page) {
                        TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
                        String age = "42";
                        // TODO unified: TestBench Escape Hatch: $(NumberFieldElement.class)
                        testBench.$(NumberFieldElement.class).withLabel("Age").single().setValue(age);
                    }
                }
                """));
    }

    @Test
    void buttonLabelAndCaptionFiltersBecomeTextWithAMarker() {
        rewriteRun(java(
                """
                import com.vaadin.flow.component.button.testbench.ButtonElement;
                import com.vaadin.testbench.BrowserTest;
                import com.vaadin.testbench.BrowserTestBase;

                class ProfileTest extends BrowserTestBase {

                    @BrowserTest
                    void saves() {
                        $(ButtonElement.class).withCaption("Save").single().click();
                        $(ButtonElement.class).withLabel("Cancel").single().click();
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
                        // TODO unified: was withCaption("Save"); use withAccessibleName(..) for a button without text
                        page.find(UnifiedButton.class).withText("Save").single().click();
                        // TODO unified: was withLabel("Cancel"); use withAccessibleName(..) for a button without text
                        page.find(UnifiedButton.class).withText("Cancel").single().click();
                    }
                }
                """));
    }
}
