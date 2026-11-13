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

    @Option(displayName = "Include page objects",
            description = "Also migrate page objects: classes that look components up through the TestBench test.",
            required = false)
    private final @Nullable Boolean includePageObjects;

    @Option(displayName = "Add the agent skill",
            description = "Also write `.claude/skills/resolve-migration-markers/SKILL.md`, a Claude Code skill that works "
                    + "through the Migration Markers the migration leaves.",
            required = false)
    private final @Nullable Boolean addAgentSkill;

    public MigrateTestBenchToUnified() {
        this(null, null, null);
    }

    public MigrateTestBenchToUnified(@Nullable Boolean addPlaywright) {
        this(addPlaywright, null, null);
    }

    public MigrateTestBenchToUnified(@Nullable Boolean addPlaywright, @Nullable Boolean includePageObjects) {
        this(addPlaywright, includePageObjects, null);
    }

    @JsonCreator
    public MigrateTestBenchToUnified(@JsonProperty("addPlaywright") @Nullable Boolean addPlaywright,
            @JsonProperty("includePageObjects") @Nullable Boolean includePageObjects,
            @JsonProperty("addAgentSkill") @Nullable Boolean addAgentSkill) {
        this.addPlaywright = addPlaywright;
        this.includePageObjects = includePageObjects;
        this.addAgentSkill = addAgentSkill;
    }

    public @Nullable Boolean getAddAgentSkill() {
        return addAgentSkill;
    }

    public @Nullable Boolean getIncludePageObjects() {
        return includePageObjects;
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
        Environment recipes = Environment.builder().load(DeclaredRecipes.load(RECIPES)).build();
        List<Recipe> list = new ArrayList<>(names.stream().map(name -> recipes.activateRecipes(name)).map(Recipe.class::cast).toList());
        list.add(new RemoveJUnit4WhenUnused()); // scans every source, so outside the build and test-code preconditions
        if (Boolean.TRUE.equals(addAgentSkill)) {
            list.add(new AddMigrationMarkersSkill());
        }
        if (Boolean.TRUE.equals(includePageObjects)) {
            list.add(1, new TestBenchPageObjectsToUnified()); // before the test code, which then passes page for this
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MigrateTestBenchToUnified other && Objects.equals(addPlaywright, other.addPlaywright)
                && Objects.equals(includePageObjects, other.includePageObjects)
                && Objects.equals(addAgentSkill, other.addAgentSkill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addPlaywright, includePageObjects, addAgentSkill);
    }
}
