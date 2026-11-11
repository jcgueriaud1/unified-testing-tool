package org.vaadin.addons.unified.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;

/**
 * The composite Migration Recipe: rewrites TestBench (Selenium) tests in place into Unified UI Tests and swaps
 * TestBench for the Unified API in the build (ADR 0004). Its parts, declared in
 * {@code META-INF/rewrite/testbench.yml}, can also run on their own.
 */
public class MigrateTestBenchToUnified extends Recipe {

    private static final String RECIPES = "/META-INF/rewrite/testbench.yml";

    @Option(displayName = "Add Playwright",
            description = "Also add the Playwright adapter (`unified-test-playwright`) as a test dependency.",
            required = false)
    private final @Nullable Boolean addPlaywright;

    public MigrateTestBenchToUnified() {
        this(null);
    }

    @JsonCreator
    public MigrateTestBenchToUnified(@JsonProperty("addPlaywright") @Nullable Boolean addPlaywright) {
        this.addPlaywright = addPlaywright;
    }

    public @Nullable Boolean getAddPlaywright() {
        return addPlaywright;
    }

    @Override
    public String getDisplayName() {
        return "Migrate TestBench tests to Unified UI Tests";
    }

    @Override
    public String getDescription() {
        return "Rewrites TestBench (Selenium) tests in place into Unified UI Tests that run on every Testing Tool, "
                + "and swaps TestBench for the Unified API in the build.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        List<String> names = new ArrayList<>(List.of("org.vaadin.addons.unified.rewrite.MigrateTestBenchBuild"));
        if (Boolean.TRUE.equals(addPlaywright)) {
            names.add("org.vaadin.addons.unified.rewrite.AddPlaywrightAdapter");
        }
        names.add("org.vaadin.addons.unified.rewrite.MigrateTestBenchTestCode");
        Environment recipes = Environment.builder().load(declaredRecipes()).build();
        return names.stream().map(name -> recipes.activateRecipes(name)).map(Recipe.class::cast).toList();
    }

    private YamlResourceLoader declaredRecipes() {
        URL url = MigrateTestBenchToUnified.class.getResource(RECIPES);
        try (InputStream in = url.openStream()) {
            return new YamlResourceLoader(in, url.toURI(), new Properties(), MigrateTestBenchToUnified.class.getClassLoader());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MigrateTestBenchToUnified other && Objects.equals(addPlaywright, other.addPlaywright);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(addPlaywright);
    }
}
