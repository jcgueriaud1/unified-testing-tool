package org.vaadin.addons.unified.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

/**
 * The composite Browserless Migration Recipe: swaps Browserless Test for the Unified API in the build (ADR 0005).
 * Its parts, declared in {@code META-INF/rewrite/browserless.yml}, can also run on their own. Without a browser
 * adapter option the migrated suite keeps running on Browserless only, as before.
 */
public class MigrateBrowserlessToUnified extends Recipe {

    @Option(displayName = "Add Playwright",
            description = "Also add the Playwright adapter (`unified-test-playwright`) as a test dependency.",
            required = false)
    private final @Nullable Boolean addPlaywright;

    @Option(displayName = "Add Selenium",
            description = "Also add the Selenium adapter (`unified-test-selenium`) as a test dependency.",
            required = false)
    private final @Nullable Boolean addSelenium;

    public MigrateBrowserlessToUnified() {
        this(null, null);
    }

    @JsonCreator
    public MigrateBrowserlessToUnified(@JsonProperty("addPlaywright") @Nullable Boolean addPlaywright,
            @JsonProperty("addSelenium") @Nullable Boolean addSelenium) {
        this.addPlaywright = addPlaywright;
        this.addSelenium = addSelenium;
    }

    public @Nullable Boolean getAddPlaywright() {
        return addPlaywright;
    }

    public @Nullable Boolean getAddSelenium() {
        return addSelenium;
    }

    @Override
    public String getDisplayName() {
        return "Migrate Browserless tests to Unified UI Tests";
    }

    @Override
    public String getDescription() {
        return "Swaps Browserless Test (and TestBench UI Unit) for the Unified API in the build.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        List<String> names = new ArrayList<>(List.of("org.vaadin.addons.unified.rewrite.MigrateBrowserlessBuild"));
        if (Boolean.TRUE.equals(addPlaywright)) {
            names.add("org.vaadin.addons.unified.rewrite.AddPlaywrightAdapter");
        }
        if (Boolean.TRUE.equals(addSelenium)) {
            names.add("org.vaadin.addons.unified.rewrite.AddSeleniumAdapter");
        }
        Environment recipes = Environment.builder()
                .load(DeclaredRecipes.load("/META-INF/rewrite/browserless.yml"))
                .load(DeclaredRecipes.load("/META-INF/rewrite/testbench.yml"))
                .build();
        return names.stream().map(recipes::activateRecipes).map(Recipe.class::cast).toList();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MigrateBrowserlessToUnified other && Objects.equals(addPlaywright, other.addPlaywright)
                && Objects.equals(addSelenium, other.addSelenium);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addPlaywright, addSelenium);
    }
}
