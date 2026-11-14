package org.vaadin.addons.unified.rewrite;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/**
 * Dramafinder 1.x sources (ADR 0006): the version comes from the build, 1.x lookups keep matching a substring, and
 * a project's copy of {@code SpringPlaywrightIT} goes once nothing extends it. The sources are attributed against
 * Dramafinder 2.x, which has the same names.
 */
class MigrateDramafinder1Test implements RewriteTest {

    private static final String UNIFIED_VERSION = System.getProperty("unified.version");

    private static final String COPIED_BASE = """
            package com.example;

            import org.springframework.boot.test.context.SpringBootTest;
            import org.springframework.boot.test.web.server.LocalServerPort;
            import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;

            @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
            public abstract class SpringPlaywrightIT extends AbstractBasePlaywrightIT {

                @LocalServerPort
                private int port;

                @Override
                public String getUrl() {
                    return "http://localhost:" + port + "/";
                }

                protected void loginAsAdmin() {
                }
            }
            """;

    private static final String TEST = """
            package com.example;

            import org.junit.jupiter.api.Test;
            import org.vaadin.addons.dramafinder.element.ButtonElement;

            class PeopleTest extends SpringPlaywrightIT {
                @Test
                void saves() {
                    ButtonElement.getByText(page, "Save").click();
                }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateDramafinderToUnified())
                .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    private static String pom(String dramafinderVersion) {
        String dependency = dramafinderVersion == null ? """
                        <dependency>
                            <groupId>org.vaadin.addons</groupId>
                            <artifactId>unified-test-playwright</artifactId>
                            <scope>test</scope>
                        </dependency>
                """ : """
                        <dependency>
                            <groupId>org.vaadin.addons</groupId>
                            <artifactId>dramafinder</artifactId>
                            <version>%s</version>
                            <scope>test</scope>
                        </dependency>
                """.formatted(dramafinderVersion);
        String bom = dramafinderVersion != null ? "" : "\n" + String.join("\n",
                "            <dependency>",
                "                <groupId>org.vaadin.addons</groupId>",
                "                <artifactId>unified-test-bom</artifactId>",
                "                <version>" + UNIFIED_VERSION + "</version>",
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
    void a1xBuildKeepsSubstringLookupsAndTheUnusedCopiedBaseGoes() {
        rewriteRun(
                pomXml(pom("1.2.4"), pom(null)),
                java(COPIED_BASE, (String) null),
                java(TEST, """
                        package com.example;

                        import com.vaadin.testing.unified.UnifiedPage;
                        import com.vaadin.testing.unified.component.UnifiedButton;
                        import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                        import org.junit.jupiter.api.Test;

                        @UnifiedSpringBootTest
                        class PeopleTest {
                            @Test
                            void saves(UnifiedPage page) {
                                page.navigate("");
                                // TODO unified: was 1.x getByText(..), a case-insensitive substring and first match; tighten it to withText(..) once the test passes
                                page.find(UnifiedButton.class).withTextContaining("Save").single().click();
                            }
                        }
                        """));
    }

    @Test
    void a2xBuildConvertsLookupsExactly() {
        rewriteRun(
                pomXml(pom("2.0.2"), pom(null)),
                java(COPIED_BASE, (String) null),
                java(TEST, """
                        package com.example;

                        import com.vaadin.testing.unified.UnifiedPage;
                        import com.vaadin.testing.unified.component.UnifiedButton;
                        import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
                        import org.junit.jupiter.api.Test;

                        @UnifiedSpringBootTest
                        class PeopleTest {
                            @Test
                            void saves(UnifiedPage page) {
                                page.navigate("");
                                page.find(UnifiedButton.class).withText("Save").single().click();
                            }
                        }
                        """));
    }

    @Test
    void aCopiedBaseATestStillExtendsStays() {
        rewriteRun(
                pomXml(pom("1.2.4"), pom(null)),
                java(COPIED_BASE),
                java("""
                        package com.example;

                        import org.junit.jupiter.api.Test;

                        class AdminTest extends SpringPlaywrightIT {
                            @Test
                            void opensAsAdmin() {
                                loginAsAdmin();
                            }
                        }
                        """));
    }
}
