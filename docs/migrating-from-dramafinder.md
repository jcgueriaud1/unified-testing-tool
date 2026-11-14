# Migrating from Dramafinder

The `unified-test-rewrite` module turns Dramafinder 1.x and 2.x tests into Unified UI Tests with [OpenRewrite](https://docs.openrewrite.org/). It rewrites your test sources and `pom.xml` in place, so review the result with `git diff`. Code it can't convert keeps running on Playwright through the Escape Hatch, with a Migration Marker (`// TODO unified: ...`) on each statement that needs a person, or an agent, to look at it. Decided in [ADR 0006](adr/0006-dramafinder-migration-recipes.md), which follows the TestBench and Browserless migrations ([ADR 0004](adr/0004-test-migration-openrewrite-recipes-then-agent-skill.md), [ADR 0005](adr/0005-browserless-migration-recipes.md)).

!!! warning "Not published yet"
    The Migration Recipes are published to Maven Central from `spec-v1`. Until then, build them from this repository with `mvn install`; the commands below then find them in your local Maven repository.

## Before and after

The demo's `EditPersonDramafinderTest`, and what the recipe makes of it. CI checks that the recipe still produces exactly this, and runs the result on Browserless, Playwright and Selenium: nothing is left behind the Escape Hatch, so the test now runs on every Testing Tool. The Migration Markers left in it are checks for a person: they don't stop the test from running.

=== "Before (Dramafinder)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonDramafinderTest.java:test"
    ```

=== "After (Unified API)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonDramafinderMigratedTest.java:test"
    ```

## Run it

Commit or stash your work first: the recipe changes files in place.

=== "Maven"

    ```bash
    mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
      -Drewrite.recipeArtifactCoordinates=org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2 \
      -Drewrite.activeRecipes=org.vaadin.addons.unified.rewrite.MigrateDramafinderToUnified
    ```

    To set options, declare your own recipe in a `rewrite.yml` at the project root and activate it instead:

    ```yaml
    type: specs.openrewrite.org/v1beta/recipe
    name: com.example.MigrateDramafinder
    recipeList:
      - org.vaadin.addons.unified.rewrite.MigrateDramafinderToUnified:
          addBrowserless: true
          addAgentSkill: true
    ```

    ```bash
    mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
      -Drewrite.recipeArtifactCoordinates=org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2 \
      -Drewrite.activeRecipes=com.example.MigrateDramafinder
    ```

=== "Gradle"

    ```kotlin
    plugins {
        id("org.openrewrite.rewrite") version "latest.release"
    }
    rewrite {
        activeRecipe("org.vaadin.addons.unified.rewrite.MigrateDramafinderToUnified")
    }
    dependencies {
        rewrite("org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2")
    }
    ```

    ```bash
    gradle rewriteRun
    ```

    The build changes are Maven only: on Gradle, add the dependencies by hand (see **The build** below). Without a Maven build the recipe can't read the Dramafinder version from it, and finds it from the classpath instead (see **Dramafinder 1.x**).

| Option | Default | Effect |
|---|---|---|
| `addBrowserless` | `false` | Also adds the Browserless adapter, `unified-test-browserless` |
| `addSelenium` | `false` | Also adds the Selenium adapter, `unified-test-selenium` |
| `addAgentSkill` | `false` | Also writes the agent skill that resolves Migration Markers (see below) |

Without another adapter the migrated suite keeps running on Playwright only, as before; adding one is the step that makes it run elsewhere (see **Running on the other Testing Tools**).

## What it converts

| Dramafinder | Unified API |
|---|---|
| `extends SpringPlaywrightIT` (with or without `@SpringBootTest`) | `@UnifiedSpringBootTest` |
| `extends AbstractBasePlaywrightIT` | `@UnifiedUITest`; `getUrl()` goes: the base URL is the `vaadin.testing.base-url` setting ([test setup](spec/02-test-setup.md)) |
| `getView()` returning `"people"` | each test that doesn't navigate first starts with `page.navigate("people")` |
| `@EnabledIfSystemProperty(named = "vaadin.testing.tool", ..)` | removed: the run's Testing Tool decides |
| test methods, `@BeforeEach` and helper methods that use the base class | a `UnifiedPage page` parameter; a `@BeforeEach` becomes a call at the start of each test |
| `navigate(View.class)`, `assertAt(View.class)`, `assertPath(..)`, `assertQueryParameter(..)` | `page.navigate(View.class)`, `assertThat(page).isAt(View.class)`, `hasPath(..)`, `hasQueryParameter(..)` |
| `TextFieldElement.getByLabel(page, "Name")`, `getByText`, `getById`, `getByTestId`, `…Containing` | `page.find(UnifiedTextField.class).withLabel("Name").single()`, `withText`, `withId`, `withTestId`, `…Containing` |
| `find(page, TextFieldElement.class).withLabel("Name").single()` | `page.find(UnifiedTextField.class).withLabel("Name").single()` |
| query `first()` / `get(i)` | `single()`, with a Migration Marker / `all().get(i)` |
| `NotificationElement.getByText(page, s)` (a substring, the first match) | `withTextContaining(s).single()`, with a Migration Marker |
| element actions: `setValue`, `click`, `check`, `closeWithEscape`, ... | the Wrapper's: `setValue`, `click`, `check`, `close`, ... |
| element assertions: `assertValue`, `assertOpen`, `assertEnabled`, `assertChecked`, `assertContent`, ... | retrying `assertThat(w).hasValue(..)`, `isOpen()`, `isEnabled()`, `isChecked()`, `hasTextContaining(..)`, ... |
| a local `TextFieldElement name = ...` | `UnifiedTextField name = page.find(..)...` |
| `GridElement.get(page)` | `page.find(UnifiedGrid.class).single()` |
| `grid.findRow(i).orElseThrow()` | `grid.row(i)` |
| `grid.findRowIndexesWithColumnText(0, s).get(0)` | `grid.rows().withCellText(s).single().getIndex()`, with a Migration Marker: any column matches now |
| `row.getCell(col).getText()`, `row.getCell("Header").assertText(s)`, `row.getCell(col).click()` | `row.getCellText(col)`, `assertThat(row).hasCellText("Header", s)`, `row.click()` with a Migration Marker |
| `AutoWait.waitForVaadinIdle(page)`, `page.waitForLoadState()`, `waitForGridToStopLoading()`, `scrollToRow(i)` | removed: Auto-Wait |

A lookup converts only when its Wrapper has every call the test makes on what it finds, and only on the test's page (a lookup inside a `Locator` or another element stays). The Wrappers the recipe knows today: TextField, TextArea, Button, Notification, Checkbox, Dialog and Grid; other components stay behind the Escape Hatch. The full list of what the recipe writes is in [Appendix: mapping](spec/appendix-mapping.md#dramafinder-calls-the-migration-recipe-converts).

A test class that uses a base class member the recipe doesn't convert (`loginAs(..)`, `newPageAs(..)`, `getBrowser()`, an override of `prepareBrowserContext(..)`, a `getView()` that computes its view) is left exactly as it is: convert those by hand (`openAsUser(..)` for another user, [users](spec/11-users.md)), then run the migration again.

### Dramafinder 1.x

Dramafinder 1.x and 2.x share their class and method names, but not what a lookup finds: a 1.x `getByLabel` or `getByText` matched a case-insensitive substring of the label, `aria-label` or placeholder and took the first match, where 2.x matches the whole value and fails on several. So the recipe first finds the version: the one your Maven build resolves; else the API on the classpath; else the API of the base class your tests were compiled against. A version it can't find counts as 1.x, since a wrong 1.x guess only adds Migration Markers.

In a 1.x project, `getByLabel(page, s)` becomes `withAccessibleNameContaining(s).single()` and `getByText(page, s)` `withTextContaining(s).single()`, each with a Migration Marker to tighten it to `withLabel`/`withText` once the test passes. A 1.x `getByHeaderText` stays behind the Escape Hatch: the Unified API has no substring header filter.

1.x projects copied `SpringPlaywrightIT` from Dramafinder's template into their own package. The recipe recognises such a copy (an abstract class extending `AbstractBasePlaywrightIT` with `@SpringBootTest`), migrates the tests that extend it as if they extended Dramafinder's, and deletes the copy once nothing extends it any more.

=== "Before (Dramafinder 1.x)"

    ```java
    class EditPersonDramafinder1Test extends SpringPlaywrightIT { // the project's copy

        @Override
        public String getView() {
            return "people";
        }

        @Test
        void editsAPersonFromTheGrid() {
            // ...
            TextFieldElement.getByLabel(page, "Name").setValue("Alan M. Turing");
            ButtonElement.getByText(page, "Save").click();
            // ...
        }
    }
    ```

=== "After (Unified API)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonDramafinder1MigratedTest.java"
    ```

### The build

With Maven, the recipe imports `unified-test-bom`, adds `unified-test-playwright` as a test dependency, and `unified-test-spring` when the project has a direct Spring Boot dependency. It removes the direct `dramafinder` dependency: the Playwright adapter brings Dramafinder.

## What it leaves for you

Code with no Unified equivalent keeps working on Playwright: the base class's `page` becomes the Playwright `Page` from `page.unwrap(Page.class)`, and Dramafinder elements and raw Playwright calls keep using it:

```java
@Test
@RequiresTestingTool(TestingTool.PLAYWRIGHT)
void savesTheProfile(UnifiedPage page) {
    Page playwright = page.unwrap(Page.class);
    page.navigate(ProfileView.class);
    // TODO unified: Dramafinder Escape Hatch: playwright.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click()
    playwright.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
}
```

`@RequiresTestingTool(PLAYWRIGHT)` skips such a test on the other Testing Tools. A fully converted test has no Capability and runs everywhere.

| Migration Marker | What to do |
|---|---|
| `Dramafinder Escape Hatch: <call>` | Rewrite the call with the Unified API: a Dramafinder lookup or a raw Playwright locator (`getByRole`, `getByLabel`, `locator("vaadin-…")`) names a component a Wrapper finds. Keep it for routes, tracing, JavaScript and CSS into a component's shadow DOM |
| `Dramafinder Escape Hatch: new X(..), a custom element: write a Wrapper for it (ADR 0003)` | `X` extends Dramafinder's `VaadinElement`: write a [Wrapper](guide/custom-wrappers.md) with its methods |
| `was 1.x getByLabel(..), ...; tighten it to withLabel(..) once the test passes` | Replace the `…Containing` filter with the exact one and run the test |
| `was findRowIndexesWithColumnText(n, ..).get(0): any column matches now; ...` | Name the column: `withCellText("Header", text)` |
| `was getCell(..).click(); a row's click() clicks the row, not that cell` | Fine when the cell holds nothing clickable; else find the component in the cell, `row.cell("Header").find(..)` |
| `was first(); single() fails when several components match`, `was getByText(..), a substring and first match; ...` | If the test now fails with "several match", add a filter |

Once done, a marker is gone, or it reads `// Escape Hatch kept: <reason>`.

## Running on the other Testing Tools

A migrated test that needs nothing Playwright-only can run on Browserless and Selenium too. Add the adapter (`addBrowserless`, `addSelenium`, or the dependency by hand) and run with `-Dvaadin.testing.tool=browserless` or `selenium`. Selenium is a real browser too, so tests that pass on Playwright usually pass there. Browserless runs in the test JVM without a browser ([limitations](spec/09-capabilities.md#limitations)); expect a few failures on the first run:

- "Visible" means `setVisible(true)` on the server: a component hidden by CSS counts as visible.
- No CSS, layout or client-side JavaScript: a test that depends on them belongs on a real browser, `@RequiresBrowser`.
- A substring lookup carried over from Dramafinder 1.x can find a second component the browser didn't show yet; tighten it as its Migration Marker says.

## The agent skill

The same `resolve-migration-markers` skill works through the Dramafinder Migration Markers: it converts raw Playwright calls and Dramafinder lookups into Wrappers, writes Wrappers for custom elements, tightens 1.x substring lookups, or keeps the Escape Hatch and replaces the marker with `// Escape Hatch kept: <reason>`. It removes `@RequiresTestingTool(PLAYWRIGHT)` only once the test passes on both Playwright and Browserless. Add `addAgentSkill: true` to the recipe's options, then, in Claude Code, in the migrated project:

```text
/resolve-migration-markers
```

How it is evaluated, and what it scores: [Migrating from TestBench](migrating-from-testbench.md#the-agent-skill).
