package org.vaadin.addons.unified.rewrite;

import static org.openrewrite.java.Assertions.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
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
                .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void profileTestBenchTestBecomesProfileMigratedTest() throws IOException {
        assertMigrates("ProfileTestBenchTest", "ProfileMigratedTest");
    }

    @Test
    void leftoversRunThroughTheEscapeHatch() throws IOException {
        assertMigrates("ProfileEscapeHatchTestBenchTest", "ProfileEscapeHatchMigratedTest");
    }

    private void assertMigrates(String input, String twin) throws IOException {
        String before = Files.readString(DEMO_TESTS.resolve(input + ".java"));
        String after = Files.readString(DEMO_TESTS.resolve(twin + ".java")).replace("class " + twin, "class " + input);
        rewriteRun(
                // the demo's view, reduced to its route: the real one needs the whole application on the classpath
                java("""
                        package demo;

                        import com.vaadin.flow.component.Component;
                        import com.vaadin.flow.router.Route;

                        @Route("profile")
                        public class ProfileView extends Component {
                        }
                        """),
                java(before, after));
    }
}
