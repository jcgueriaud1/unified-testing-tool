# Migrating from TestBench

The `unified-test-rewrite` module turns TestBench (Selenium) tests into Unified UI Tests with [OpenRewrite](https://docs.openrewrite.org/). It rewrites your test sources and `pom.xml` in place, so review the result with `git diff`. Code it can't convert keeps running on Selenium through the Escape Hatch, with a Migration Marker (`// TODO unified: ...`) on each statement that needs a person, or an agent, to look at it. Decided in [ADR 0004](adr/0004-test-migration-openrewrite-recipes-then-agent-skill.md).

!!! warning "Not published yet"
    The Migration Recipes are published to Maven Central from `spec-v1`. Until then, build them from this repository with `mvn install`; the commands below then find them in your local Maven repository.

## Before and after

The demo's `EditPersonTestBenchTest`, and what the recipe makes of it. CI checks that the recipe still produces exactly this, and runs the result on Browserless, Playwright and Selenium.

=== "Before (TestBench)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonTestBenchTest.java:test"
    ```

=== "After (Unified API)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonMigratedTest.java:test"
    ```

## Run it

Commit or stash your work first: the recipe changes files in place.

=== "Maven"

    ```bash
    mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
      -Drewrite.recipeArtifactCoordinates=org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2 \
      -Drewrite.activeRecipes=org.vaadin.addons.unified.rewrite.MigrateTestBenchToUnified
    ```

    To set options, declare your own recipe in a `rewrite.yml` at the project root and activate it instead:

    ```yaml
    type: specs.openrewrite.org/v1beta/recipe
    name: com.example.MigrateTestBench
    recipeList:
      - org.vaadin.addons.unified.rewrite.MigrateTestBenchToUnified:
          addPlaywright: true
          includePageObjects: true
          addAgentSkill: true
    ```

    ```bash
    mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
      -Drewrite.recipeArtifactCoordinates=org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2 \
      -Drewrite.activeRecipes=com.example.MigrateTestBench
    ```

=== "Gradle"

    ```kotlin
    plugins {
        id("org.openrewrite.rewrite") version "latest.release"
    }
    rewrite {
        activeRecipe("org.vaadin.addons.unified.rewrite.MigrateTestBenchToUnified")
    }
    dependencies {
        rewrite("org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2")
    }
    ```

    ```bash
    gradle rewriteRun
    ```

    The build changes are Maven only: on Gradle, add the dependencies by hand (see **The build** below).

| Option | Default | Effect |
|---|---|---|
| `addPlaywright` | `false` | Also adds the Playwright adapter, `unified-test-playwright` |
| `includePageObjects` | `false` | Also converts page objects (see below) |
| `addAgentSkill` | `false` | Also writes the agent skill that resolves Migration Markers (see below) |

Then run your tests on each Testing Tool (`-Dvaadin.testing.tool=browserless`, `playwright` or `selenium`) and work through the Migration Markers, by hand or with the agent skill.

## What it converts

| TestBench | Unified API |
|---|---|
| `extends BrowserTestBase`, `TestBenchTestCase` or `ParallelTest`; `@BrowserTest` | `@UnifiedSpringBootTest` (from `@SpringBootTest`) or `@UnifiedUITest`; `@Test` with a `UnifiedPage page` parameter |
| `getDriver().get("http://localhost:" + port + "/people")` | `page.navigate(PeopleView.class)` when one view has the route, else `page.navigate("people")` |
| `$(TextFieldElement.class).withLabel("Name").single()` | `page.find(UnifiedTextField.class).withLabel("Name").single()` |
| `first()`, `waitForFirst()` | `single()`, with a Migration Marker: `single()` fails when several components match |
| `Assertions.assertEquals(x, el.getValue())`, `getText()`, `assertTrue(cb.isChecked())`, ... | retrying `assertThat(el).hasValue(x)`, `hasText(x)`, `isChecked()`, ... |
| a local `TextFieldElement name = ...` | `UnifiedTextField name = ...` |
| `@BeforeEach` and helper methods that use TestBench | helpers taking `UnifiedPage page`; a `@BeforeEach` becomes a call at the start of each test |
| a project base class extending `BrowserTestBase` | a plain base class with `@UnifiedSpringBootTest`; its tests and helpers convert as above |
| Grid `getCell("text")`, `getCell(row, col).getText()` | `rows().withCellText("text").single()`, `row(row).getCellText(col)` |
| ComboBox / RadioButtonGroup / MultiSelectComboBox `selectByText`, `getSelectedText`, ... | `selectItem`, `getSelectedItem`, ... |
| DatePicker `setDate`, `getDate` | `setValue`, `getValue` |
| NumberField `setValue("42")` | `setValue(42.0)` (numeric string literals only) |
| `dialog.$(X.class)`, `tabs.getContent().$(X.class)` | `dialog.find(UnifiedX.class)`, `tabs.content().find(UnifiedX.class)` |
| TabSheet `setSelectedTabIndex(t.getTab("Security"))` | `selectTab("Security")` |
| Button `withLabel(..)`, `withCaption(..)` | `withText(..)`, with a Migration Marker suggesting `withAccessibleName(..)` for a button without text |
| JUnit 4: `@Before`, `@Test`, `Assert`, `@Ignore`, `ExpectedException` | JUnit 6 |
| `setDriver(..)`, `getDriver().quit()`, `ScreenshotOnFailureRule`, `@RunWith(SpringRunner.class)` | removed: the Unified API owns the browser and writes failure evidence |

The full mapping is in [Appendix: mapping](spec/appendix-mapping.md).

### Page objects

With `includePageObjects`, a page object that holds the TestBench test (`HasElementQuery`, `TestBenchTestCase`, ...) takes the `UnifiedPage` instead: its lookups become `find(..)`, its methods return Wrappers, and tests pass `page` where they passed `this`. It converts only when every lookup in it, and every use its callers make of what it returns, converts. Otherwise, and without the option, `new LoginPage(this)` becomes `new LoginPage(testBench)` behind the Escape Hatch.

### The build

With Maven, the recipe imports `unified-test-bom`, adds `unified-test-spring`, `unified-test-browserless` and `unified-test-selenium` as test dependencies, and removes direct `vaadin-testbench*` dependencies (the Selenium adapter brings TestBench). It removes `junit:junit` and `junit-vintage-engine` only when no JUnit 4 test is left after the migration: JUnit 6 runs JUnit 4 tests only through the vintage engine.

## What it leaves for you

Code with no Unified equivalent keeps working on Selenium through `TestBenchEscapeHatch`, TestBench's test base API on the Unified Page's browser:

```java
@Test
@RequiresTestingTool(TestingTool.SELENIUM)
void knowsTheArchiveButtonIsDisabled(UnifiedPage page) {
    TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
    // TODO unified: TestBench Escape Hatch: $(ButtonElement.class)
    Assertions.assertTrue(testBench.$(ButtonElement.class).withText("Archive").single().hasAttribute("disabled"));
}
```

`@RequiresTestingTool(SELENIUM)` skips such a test on the other Testing Tools. A fully converted test has no Capability and runs everywhere.

| Migration Marker | What to do |
|---|---|
| `TestBench Escape Hatch: <call>` | Rewrite the call with the Unified API if it can express it, then drop the Escape Hatch and `@RequiresTestingTool` |
| `..., a custom element: write a Wrapper for it (ADR 0003)` | Write a [Wrapper](spec/06-wrappers.md) for the component |
| `was first(); single() fails when several components match` | If the test now fails with "several match", add a filter |
| `was withCaption(..); use withAccessibleName(..) for a button without text` | Check the button has visible text |
| `TestBench page object: new LoginPage(this)` | Convert the page object by hand, or rerun with `includePageObjects` |
| `browser setup removed (@RunLocally, ...)` | Configure the Selenium adapter with `vaadin.testing.*` settings |

Once done, a marker is gone, or it reads `// Escape Hatch kept: <reason>`.

## The agent skill

`resolve-migration-markers` is a [Claude Code](https://claude.com/claude-code) skill that works through the Migration Markers: it resolves each one into Unified API code, or keeps the Escape Hatch and replaces the marker with `// Escape Hatch kept: <reason>`. It runs the tests it changed on Browserless and Playwright, and reports what it did.

Add `addAgentSkill: true` to the recipe's options (see **Run it**): the migration then also writes `.claude/skills/resolve-migration-markers/SKILL.md`, the version matching your recipe's markers. In Claude Code, in the migrated project:

```text
/resolve-migration-markers
```

Review its changes like any other. The skill's source is in [`unified-test-rewrite`](https://github.com/parttio/unified-testing-tool/blob/main/unified-test-rewrite/src/main/resources/META-INF/unified/skills/resolve-migration-markers/SKILL.md).

### Evaluating the skill

`scripts/eval-migration-skill.sh` runs the skill with Claude Code on the demo's migrated tests in a scratch worktree, runs them on Browserless and Playwright, and scores the result: markers resolved, kept and untouched, Escape Hatches and single-tool tests left, tests passing on each Testing Tool. It compares the score with the committed baseline (`unified-test-rewrite/skill-eval/baseline.json`) and writes an HTML or Markdown report (`--format html|md`).

The comparison is fixture by fixture, over the fixtures in both the baseline and the run. A fixture is a migrated test that still has Migration Markers, so when a recipe improves and a fixture loses its last marker, it leaves the evaluation: the report lists it as "no longer needs the skill" instead of counting the run as worse. A new fixture is reported and compared against nothing. Markers resolved and untouched are compared as rates of the fixture's markers, so a fixture whose markers changed is still compared like with like; Escape Hatches, single-tool tests and tests passed or failed on each Testing Tool are compared as counts. The report shows the totals with their rates (markers resolved / markers before, tests passed / tests run) and a row per fixture.

A run is acceptable when no compared metric is worse, and, when the skill changed, at least one is better, unless the baseline has nothing left to improve (every compared fixture with all its markers resolved, no Escape Hatch, no single-tool test, no failing test); `--accept` then commits it as the new baseline. CI can't run Claude Code, so when the skill changes on `main` without a new baseline, a workflow opens an issue to run it.
