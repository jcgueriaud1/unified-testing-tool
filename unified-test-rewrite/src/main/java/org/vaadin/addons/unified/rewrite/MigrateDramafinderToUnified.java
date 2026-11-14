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
 * The composite Dramafinder Migration Recipe: rewrites Dramafinder tests in place into Unified UI Tests and swaps
 * Dramafinder for the Unified API's Playwright adapter in the build (ADR 0006).
 * Its parts, declared in {@code META-INF/rewrite/dramafinder.yml}, can also run on their own. Without an adapter
 * option the migrated suite keeps running on Playwright only, as before.
 */
public class MigrateDramafinderToUnified extends Recipe {

    @Option(displayName = "Add Browserless",
            description = "Also add the Browserless adapter (`unified-test-browserless`) as a test dependency.",
            required = false)
    private final @Nullable Boolean addBrowserless;

    @Option(displayName = "Add Selenium",
            description = "Also add the Selenium adapter (`unified-test-selenium`) as a test dependency.",
            required = false)
    private final @Nullable Boolean addSelenium;

    @Option(displayName = "Add the agent skill",
            description = "Also write `.claude/skills/resolve-migration-markers/SKILL.md`, a Claude Code skill that works "
                    + "through the Migration Markers the migration leaves.",
            required = false)
    private final @Nullable Boolean addAgentSkill;

    public MigrateDramafinderToUnified() {
        this(null, null, null);
    }

    @JsonCreator
    public MigrateDramafinderToUnified(@JsonProperty("addBrowserless") @Nullable Boolean addBrowserless,
            @JsonProperty("addSelenium") @Nullable Boolean addSelenium,
            @JsonProperty("addAgentSkill") @Nullable Boolean addAgentSkill) {
        this.addBrowserless = addBrowserless;
        this.addSelenium = addSelenium;
        this.addAgentSkill = addAgentSkill;
    }

    public @Nullable Boolean getAddBrowserless() {
        return addBrowserless;
    }

    public @Nullable Boolean getAddSelenium() {
        return addSelenium;
    }

    public @Nullable Boolean getAddAgentSkill() {
        return addAgentSkill;
    }

    @Override
    public String getDisplayName() {
        return "Migrate Dramafinder tests to Unified UI Tests";
    }

    @Override
    public String getDescription() {
        return "Rewrites Dramafinder tests in place into Unified UI Tests and swaps Dramafinder for the Unified API's "
                + "Playwright adapter in the build.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        List<String> names = new ArrayList<>(List.of("org.vaadin.addons.unified.rewrite.MigrateDramafinderBuild"));
        if (Boolean.TRUE.equals(addBrowserless)) {
            names.add("org.vaadin.addons.unified.rewrite.AddBrowserlessAdapter");
        }
        if (Boolean.TRUE.equals(addSelenium)) {
            names.add("org.vaadin.addons.unified.rewrite.AddSeleniumAdapter");
        }
        names.add("org.vaadin.addons.unified.rewrite.MigrateDramafinderTestCode");
        Environment recipes = Environment.builder()
                .load(DeclaredRecipes.load("/META-INF/rewrite/dramafinder.yml"))
                .load(DeclaredRecipes.load("/META-INF/rewrite/browserless.yml"))
                .build();
        List<Recipe> list = new ArrayList<>(names.stream().map(recipes::activateRecipes).map(Recipe.class::cast).toList());
        if (Boolean.TRUE.equals(addAgentSkill)) {
            list.add(new AddMigrationMarkersSkill());
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MigrateDramafinderToUnified other && Objects.equals(addBrowserless, other.addBrowserless)
                && Objects.equals(addSelenium, other.addSelenium) && Objects.equals(addAgentSkill, other.addAgentSkill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addBrowserless, addSelenium, addAgentSkill);
    }
}
