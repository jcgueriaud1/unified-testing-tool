package org.vaadin.addons.unified.rewrite;

import static org.openrewrite.java.Assertions.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/**
 * The recipe's output on the demo's {@code ProfileTestBenchTest} is exactly the demo's {@code ProfileMigratedTest},
 * and likewise for the other golden pairs (Browserless, Dramafinder).
 * The demo's CI jobs run that twin on Browserless, Playwright and Selenium, so the output is proven to compile and
 * pass on every Testing Tool.
 */
class GoldenMigrationTest implements RewriteTest {

    private static final Path DEMO_TESTS = Path.of("../unified-test-demo/src/test/java/demo");

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.vaadin.addons.unified.rewrite").build()
                        .activateRecipes("org.vaadin.addons.unified.rewrite.MigrateTestBenchToUnified"))
                .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath())
                        // the demo never folds imports into a wildcard; a rewrite plugin run detects that from the project
                        .styles(List.of(new NamedStyles(Tree.randomId(), "demo", "demo", "demo", Set.of(),
                                List.of(ImportLayoutStyle.builder()
                                        .classCountToUseStarImport(999)
                                        .nameCountToUseStarImport(999)
                                        .importAllOthers()
                                        .blankLine()
                                        .importPackage("javax.*")
                                        .importPackage("java.*")
                                        .blankLine()
                                        .importStaticAllOthers()
                                        .build())))));
    }

    @Test
    void profileTestBenchTestBecomesProfileMigratedTest() throws IOException {
        assertMigrates("ProfileTestBenchTest", "ProfileMigratedTest");
    }

    @Test
    void leftoversRunThroughTheEscapeHatch() throws IOException {
        assertMigrates("ProfileEscapeHatchTestBenchTest", "ProfileEscapeHatchMigratedTest");
    }

    @Test
    void editPersonTestBenchTestBecomesEditPersonMigratedTest() throws IOException {
        assertMigrates("EditPersonTestBenchTest", "EditPersonMigratedTest");
    }

    @Test
    void fieldsTestBenchTestBecomesFieldsMigratedTest() throws IOException {
        assertMigrates("FieldsTestBenchTest", "FieldsMigratedTest");
    }

    @Test
    void containersTestBenchTestBecomesContainersMigratedTest() throws IOException {
        assertMigrates("ContainersTestBenchTest", "ContainersMigratedTest");
    }

    @Test
    void aJUnit4TestBenchTestMigratesToJUnit6() throws IOException {
        assertMigrates("JUnit4TestBenchTest", "JUnit4MigratedTest");
    }

    @Test
    void aBaseClassASubclassAndAPageObjectMigrateTogether() throws IOException {
        // twin names in the after files are the input names, as the recipe can't rename classes
        java.util.Map<String, String> twins = java.util.Map.of(
                "AbstractProfileTestBench", "AbstractProfileMigrated",
                "ProfileTestBenchPage", "ProfileMigratedPage",
                "ProfileSubclassTestBenchTest", "ProfileSubclassMigratedTest");
        List<org.openrewrite.test.SourceSpecs> sources = new java.util.ArrayList<>(views());
        for (var pair : twins.entrySet()) {
            String after = Files.readString(DEMO_TESTS.resolve(pair.getValue() + ".java"));
            for (var name : twins.entrySet()) {
                after = after.replace(name.getValue(), name.getKey());
            }
            String before = Files.readString(DEMO_TESTS.resolve(pair.getKey() + ".java"));
            sources.add(java(before, after));
        }
        rewriteRun(spec -> spec.recipe(new MigrateTestBenchToUnified(null, true)),
                sources.toArray(org.openrewrite.test.SourceSpecs[]::new));
    }

    /** The demo's views, reduced to their routes: the real ones need the whole application on the classpath. */
    private static List<org.openrewrite.test.SourceSpecs> views() {
        return List.of(view("ProfileView", "profile"), view("PeopleView", "people"),
                view("PreferencesView", "preferences"), view("OrderView", "order"), view("AccountView", "account"),
                view("AddonView", "addon"));
    }

    private static org.openrewrite.test.SourceSpecs view(String name, String route) {
        // PeopleView's grid items, for the context-API test that names them
        String body = name.equals("PeopleView") ? "\n    public record Person(String name, String email) {\n    }\n" : "";
        return java("""
                package demo;

                import com.vaadin.flow.component.Component;
                import com.vaadin.flow.router.Route;

                @Route("%s")
                public class %s extends Component {%s}
                """.formatted(route, name, body));
    }

    @Test
    void profileBrowserlessTestBecomesProfileBrowserlessMigratedTest() throws IOException {
        assertMigratesBrowserless("ProfileBrowserlessTest", "ProfileBrowserlessMigratedTest");
    }

    @Test
    void browserlessLeftoversRunThroughTheEscapeHatch() throws IOException {
        assertMigratesBrowserless("ProfileBrowserlessLeftoversTest", "ProfileBrowserlessLeftoversMigratedTest");
    }

    @Test
    void aNarrowSpringContextStaysOnBrowserless() throws IOException {
        assertMigratesBrowserless("ProfileBrowserlessNarrowContextTest", "ProfileBrowserlessNarrowContextMigratedTest");
    }

    @Test
    void aContextApiTestBecomesEditPersonBrowserlessMigratedTest() throws IOException {
        assertMigratesBrowserless("EditPersonBrowserlessTest", "EditPersonBrowserlessMigratedTest");
    }

    @Test
    void aBrowserlessTestAndItsPageObjectMigrateTogether() throws IOException {
        // twin names in the after files are the input names, as the recipe can't rename classes
        java.util.Map<String, String> twins = java.util.Map.of(
                "ProfileBrowserlessPage", "ProfileBrowserlessMigratedPage",
                "ProfileBrowserlessPageObjectTest", "ProfileBrowserlessPageObjectMigratedTest");
        List<org.openrewrite.test.SourceSpecs> sources = new java.util.ArrayList<>(views());
        for (var pair : twins.entrySet()) {
            String after = Files.readString(DEMO_TESTS.resolve(pair.getValue() + ".java"));
            for (var name : twins.entrySet()) {
                after = after.replace(name.getValue(), name.getKey());
            }
            sources.add(java(Files.readString(DEMO_TESTS.resolve(pair.getKey() + ".java")), after));
        }
        rewriteRun(spec -> spec.recipe(new MigrateBrowserlessToUnified(null, null, true, null)),
                sources.toArray(org.openrewrite.test.SourceSpecs[]::new));
    }

    @Test
    void aTestBenchUiUnitTestMigratesLikeItsBrowserlessTwin() throws IOException {
        // TestBench UI Unit 9.x needs Vaadin 24, so its golden source is no demo test: compile-only, here
        String before = new String(getClass().getResourceAsStream("/golden/ProfileUIUnitTest.java").readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        String after = Files.readString(DEMO_TESTS.resolve("ProfileBrowserlessMigratedTest.java"))
                .replace("class ProfileBrowserlessMigratedTest", "class ProfileUIUnitTest");
        List<org.openrewrite.test.SourceSpecs> sources = new java.util.ArrayList<>(views());
        sources.add(java(before, after));
        rewriteRun(spec -> spec.recipe(new MigrateBrowserlessToUnified()).parser(UiUnit.parser()),
                sources.toArray(org.openrewrite.test.SourceSpecs[]::new));
    }

    /** The Browserless input becomes the twin. */
    private void assertMigratesBrowserless(String input, String twin) throws IOException {
        String before = Files.readString(DEMO_TESTS.resolve(input + ".java"));
        String after = Files.readString(DEMO_TESTS.resolve(twin + ".java")).replace("class " + twin, "class " + input);
        List<org.openrewrite.test.SourceSpecs> sources = new java.util.ArrayList<>(views());
        sources.add(java("""
                package demo;

                public class ProfileService {
                }
                """));
        sources.add(java(before, after));
        rewriteRun(spec -> spec.recipe(new MigrateBrowserlessToUnified()),
                sources.toArray(org.openrewrite.test.SourceSpecs[]::new));
    }

    @Test
    void editPersonDramafinderTestBecomesEditPersonDramafinderMigratedTest() throws IOException {
        String input = "EditPersonDramafinderTest";
        String twin = "EditPersonDramafinderMigratedTest";
        String before = Files.readString(DEMO_TESTS.resolve(input + ".java"));
        String after = Files.readString(DEMO_TESTS.resolve(twin + ".java")).replace("class " + twin, "class " + input);
        List<org.openrewrite.test.SourceSpecs> sources = new java.util.ArrayList<>(views());
        sources.add(java(before, after));
        rewriteRun(spec -> spec.recipe(new MigrateDramafinderToUnified()),
                sources.toArray(org.openrewrite.test.SourceSpecs[]::new));
    }

    @Test
    void aDramafinder1TestAndItsCopiedBaseClassMigrate() throws IOException {
        // Dramafinder 1.x and 2.x can't share the demo's classpath, so the 1.x golden source is no demo test
        String before = resource("/golden/dramafinder1/EditPersonDramafinder1Test.java");
        String after = Files.readString(DEMO_TESTS.resolve("EditPersonDramafinder1MigratedTest.java"))
                .replace("class EditPersonDramafinder1MigratedTest", "class EditPersonDramafinder1Test");
        List<org.openrewrite.test.SourceSpecs> sources = new java.util.ArrayList<>(views());
        // the build says 1.x; the classpath the sources are attributed against is 2.x, which has the same names
        sources.add(org.openrewrite.maven.Assertions.pomXml(dramafinderPom(true), dramafinderPom(false)));
        sources.add(java(resource("/golden/dramafinder1/SpringPlaywrightIT.java"), (String) null));
        sources.add(java(before, after));
        rewriteRun(spec -> spec.recipe(new MigrateDramafinderToUnified()),
                sources.toArray(org.openrewrite.test.SourceSpecs[]::new));
    }

    private String resource(String name) throws IOException {
        return new String(getClass().getResourceAsStream(name).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** A Spring Boot project on Dramafinder 1.2.4, or what the build recipe makes of it. */
    private static String dramafinderPom(boolean before) {
        String dependency = before ? """
                        <dependency>
                            <groupId>org.vaadin.addons</groupId>
                            <artifactId>dramafinder</artifactId>
                            <version>1.2.4</version>
                            <scope>test</scope>
                        </dependency>
                """ : """
                        <dependency>
                            <groupId>org.vaadin.addons</groupId>
                            <artifactId>unified-test-playwright</artifactId>
                            <scope>test</scope>
                        </dependency>
                """;
        String bom = before ? "" : "\n" + String.join("\n",
                "            <dependency>",
                "                <groupId>org.vaadin.addons</groupId>",
                "                <artifactId>unified-test-bom</artifactId>",
                "                <version>" + System.getProperty("unified.version") + "</version>",
                "                <type>pom</type>",
                "                <scope>import</scope>",
                "            </dependency>");
        return """
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
                            </dependency>%s
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                %s    </dependencies>
                </project>
                """.formatted(bom, dependency);
    }

    @Test
    void aCustomElementStaysBehindTheEscapeHatch() throws IOException {
        assertMigrates("RatingTestBenchTest", "RatingMigratedTest", "RatingElement");
    }

    @Test
    void aPageObjectWithoutIncludePageObjectsStaysBehindTheEscapeHatch() throws IOException {
        assertMigrates("ProfilePageObjectTestBenchTest", "ProfilePageObjectMigratedTest", "ProfileTestBenchPage");
    }

    private void assertMigrates(String input, String twin) throws IOException {
        assertMigrates(input, twin, new String[0]);
    }

    /** The input becomes the twin; the other demo test files it uses are parsed and stay as they are. */
    private void assertMigrates(String input, String twin, String... unchanged) throws IOException {
        String before = Files.readString(DEMO_TESTS.resolve(input + ".java"));
        String after = Files.readString(DEMO_TESTS.resolve(twin + ".java")).replace("class " + twin, "class " + input);
        List<org.openrewrite.test.SourceSpecs> sources = new java.util.ArrayList<>(views());
        for (String name : unchanged) {
            sources.add(java(Files.readString(DEMO_TESTS.resolve(name + ".java"))));
        }
        sources.add(java(before, after));
        rewriteRun(sources.toArray(org.openrewrite.test.SourceSpecs[]::new));
    }
}
