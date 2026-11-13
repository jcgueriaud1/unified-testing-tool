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
 * The recipe's output on the demo's {@code ProfileTestBenchTest} is exactly the demo's {@code ProfileMigratedTest}.
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
        return java("""
                package demo;

                import com.vaadin.flow.component.Component;
                import com.vaadin.flow.router.Route;

                @Route("%s")
                public class %s extends Component {
                }
                """.formatted(route, name));
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
