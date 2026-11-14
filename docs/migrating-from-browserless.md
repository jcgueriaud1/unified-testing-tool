# Migrating from Browserless

The `unified-test-rewrite` module turns Browserless Test tests, and older TestBench UI Unit tests, into Unified UI Tests with [OpenRewrite](https://docs.openrewrite.org/). It rewrites your test sources and `pom.xml` in place, so review the result with `git diff`. Code it can't convert keeps running on Browserless through the Escape Hatch, with a Migration Marker (`// TODO unified: ...`) on each statement that needs a person, or an agent, to look at it. Decided in [ADR 0005](adr/0005-browserless-migration-recipes.md), which follows the TestBench migration ([ADR 0004](adr/0004-test-migration-openrewrite-recipes-then-agent-skill.md)).

!!! warning "Not published yet"
    The Migration Recipes are published to Maven Central from `spec-v1`. Until then, build them from this repository with `mvn install`; the commands below then find them in your local Maven repository.

## Before and after

The demo's `ProfileBrowserlessTest`, and what the recipe makes of it. CI checks that the recipe still produces exactly this, and runs the result on Browserless, Playwright and Selenium. The second test stays on Browserless until its Migration Markers are resolved: `isUsable()` has no Unified form the recipe can be sure of.

=== "Before (Browserless)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/ProfileBrowserlessTest.java:test"
    ```

=== "After (Unified API)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/ProfileBrowserlessMigratedTest.java:test"
    ```

## Run it

Commit or stash your work first: the recipe changes files in place.

=== "Maven"

    ```bash
    mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
      -Drewrite.recipeArtifactCoordinates=org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2 \
      -Drewrite.activeRecipes=org.vaadin.addons.unified.rewrite.MigrateBrowserlessToUnified
    ```

    To set options, declare your own recipe in a `rewrite.yml` at the project root and activate it instead:

    ```yaml
    type: specs.openrewrite.org/v1beta/recipe
    name: com.example.MigrateBrowserless
    recipeList:
      - org.vaadin.addons.unified.rewrite.MigrateBrowserlessToUnified:
          addPlaywright: true
          addAgentSkill: true
    ```

    ```bash
    mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
      -Drewrite.recipeArtifactCoordinates=org.vaadin.addons:unified-test-rewrite:0.1.0-alpha2 \
      -Drewrite.activeRecipes=com.example.MigrateBrowserless
    ```

=== "Gradle"

    ```kotlin
    plugins {
        id("org.openrewrite.rewrite") version "latest.release"
    }
    rewrite {
        activeRecipe("org.vaadin.addons.unified.rewrite.MigrateBrowserlessToUnified")
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
| `addSelenium` | `false` | Also adds the Selenium adapter, `unified-test-selenium` |
| `addAgentSkill` | `false` | Also writes the agent skill that resolves Migration Markers (see below) |

Without a browser adapter the migrated suite keeps running on Browserless only, as before; adding one is the step that makes it run in a browser (see **Running in a browser**).

## What it converts

| Browserless | Unified API |
|---|---|
| `extends SpringBrowserlessTest` with `@SpringBootTest` | `@UnifiedSpringBootTest`; `@MockitoBean` and `@WithMockUser` stay |
| `extends BrowserlessTest` | `@UnifiedUITest`; `@ViewPackages` stays |
| test methods, `@BeforeEach` and helper methods that use Browserless | a `UnifiedPage page` parameter; a `@BeforeEach` becomes a call at the start of each test; a project base class converts too |
| `navigate(ProfileView.class)`, `UI.getCurrent().navigate(..)` | `page.navigate(ProfileView.class)` |
| `$(TextField.class).withCaption("Name").single()` | `page.find(UnifiedTextField.class).withLabel("Name").single()` |
| `test(component).setValue("x")`, `.click()`, `.check()` | `component.setValue("x")`, `.click()`, `.check()` |
| component getters: `getValue()`, `getText()`, `isEnabled()`, Checkbox `getValue()`, Dialog `isOpened()` | Wrapper getters: `getValue()`, `getText()`, `isEnabled()`, `isChecked()`, `isOpen()` |
| a local `TextField name = $(..)...` | `UnifiedTextField name = page.find(..)...` |
| `first()`, `last()`, `atIndex(n)` | `single()`, with a Migration Marker: `single()` fails when several components match |
| Button `withCaption(..)` | `withText(..)`, with a Migration Marker suggesting `withAccessibleName(..)` for a button without text |
| `Assertions.assertEquals(x, c.getValue())`, `test(c).getText()`, `assertTrue(c.isEnabled())`, ... | retrying `assertThat(c).hasValue(x)`, `hasText(x)`, `isEnabled()`, ... |
| `assertTrue(getCurrentView() instanceof V)`, `assertInstanceOf(V.class, getCurrentView())` | `assertThat(page).isAt(V.class)` |
| `roundTrip()`, `runPendingSignalsTasks()` | removed: Auto-Wait |
| `fireShortcut(Key.KEY_S, KeyModifier.CONTROL)` | `page.pressKey("Control+s")`, with a Migration Marker (see below) |
| Grid `size()`, `getCellText(row, col)`, `clickRow(row)`, `select(row)` | `getRowCount()`, `row(row).getCellText(col)`, `row(row).click()`, `row(row).select()` |
| a local `GridTester<..> grid = test(..)` or `GridLocator<..> grid = window.findGrid(..)` | `UnifiedGrid grid = page.find(UnifiedGrid.class).single()` |
| ComboBox / RadioButtonGroup `selectItem`, MultiSelectComboBox `selectItem(..)`, TabSheet `select("tab")`, Details `openDetails()` | `selectItem`, `selectItems(..)`, `selectTab("tab")`, `open()` |

A lookup converts only when its Wrapper has every call the test makes on what it finds. The Wrappers the recipe knows today: TextField, TextArea, NumberField, IntegerField, DatePicker, ComboBox, MultiSelectComboBox, RadioButtonGroup, Checkbox, Button, Notification, Dialog, TabSheet, Details and Grid; other components stay behind the Escape Hatch. The full mapping between the Testing Tools is in [Appendix: mapping](spec/appendix-mapping.md).

### Context API and extension tests

Tests written against Browserless Test's context API (a `BrowserlessUIContext` window from `BrowserlessApplicationContext`, often built in a `@TestConfiguration`) or with `@RegisterExtension BrowserlessExtension` convert the same way. Their wiring goes: the context and extension fields, the `@TestConfiguration` that builds the application, and the `@BeforeEach`/`@AfterEach` methods that only open and close the window. Calls on the window or extension convert like the base-class calls above, and its locators become lookups: `window.findTextField().withLabel("Name").setValue("x")` becomes `page.find(UnifiedTextField.class).withLabel("Name").single().setValue("x")`. Views the wiring named (`create(View.class)`, `withViewPackages(..)`) become `@ViewPackages` on a plain test. A test that uses the application or user context beyond opening its window (a second user, a second window) is left as it is, with a Migration Marker.

The demo's `EditPersonBrowserlessTest` and what the recipe makes of it. Nothing is left behind the Escape Hatch, so CI runs the result on Browserless, Playwright and Selenium.

=== "Before (context API)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonBrowserlessTest.java:test"
    ```

=== "After (Unified API)"

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonBrowserlessMigratedTest.java:test"
    ```

### TestBench UI Unit tests

Tests written with TestBench UI Unit 9.x for Vaadin 24 (`UIUnitTest`, `SpringUIUnitTest`, and the JUnit 4 `UIUnit4Test`) first move to their Browserless Test equivalents, then convert as above: `com.vaadin.testbench.unit` becomes `com.vaadin.browserless`, the HTML testers move out of `com.vaadin.flow.component.html.testbench`, and JUnit 4 tests become JUnit 6. Run the migration after moving the application to Vaadin 25.

### The build

With Maven, the recipe imports `unified-test-bom`, adds `unified-test-browserless` as a test dependency, and `unified-test-spring` when the project has a direct Spring Boot dependency. It removes direct `browserless-test*` and `vaadin-testbench-unit*` dependencies (the Browserless adapter brings Browserless Test) and keeps `browserless-test-spring`, which Spring tests need on Browserless. It removes `junit:junit` and `junit-vintage-engine` only when no JUnit 4 test is left.

## What it leaves for you

Code with no Unified equivalent keeps working on Browserless through `BrowserlessEscapeHatch`, Browserless Test's test base API on the Unified Page's window:

```java
@Test
@RequiresTestingTool(TestingTool.BROWSERLESS)
void readsTheView(UnifiedPage page) {
    BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
    // TODO unified: Component Reference: test(name).setValue("Alice"): replace it with a locator that finds the same component
    browserless.test(name).setValue("Alice");
}
```

In a context-API test, a call on the window that doesn't convert keeps the window, under its old name, from `page.unwrap(BrowserlessUIContext.class)`. `@RequiresTestingTool(BROWSERLESS)` skips such a test on the other Testing Tools. A fully converted test has no Capability and runs everywhere.

| Migration Marker | What to do |
|---|---|
| `Browserless Escape Hatch: <call>` | Rewrite the call with the Unified API if it can express it, then drop the Escape Hatch and `@RequiresTestingTool` |
| `..., no Wrapper for X: write one (ADR 0003)` | Write a [Wrapper](guide/custom-wrappers.md) for the component |
| `Component Reference: <call>: replace it with a locator ...` | The test holds a component's Java object (a view field, `view.getChildren()...`): find the component with a locator instead |
| `filter runs Java on the component: <call>: replace withCondition(..) with a locator` | `withCondition(..)` and `withPropertyValue(..)` run Java on each component: pick a label, text or test id that finds the same one |
| `was first(); single() fails when several components match` | If the test now fails with "several match", add a filter |
| `was withCaption(..); use withAccessibleName(..) for a button without text` | Check the button has visible text |
| `was fireShortcut(..); pressKey also reaches the focused component and an open dialog` | `fireShortcut` fired the shortcut on the UI only; `pressKey` also reaches the focused component and an open dialog (Escape closes it). Check the test still means the same |
| `Browserless-only pattern: @Transactional ...` | The test's transaction reaches the views only on Browserless ([test data](spec/02-test-setup.md)): keep it, or commit test data and clean up |
| `Browserless-only pattern: a Spring context without @SpringBootTest ...` | A narrow `@ContextConfiguration` has no web server: keep it, or switch to `@UnifiedSpringBootTest` with `@MockitoBean` |
| `not migrated: this test uses the Browserless application or user context ...` | The class is left as it is: use `page.openWindow()` or `page.openAsUser(..)` for the second window or user, then run the migration again |
| `not migrated: this class passes itself (this) to a page object or helper ...` | The class is left as Browserless Test: make the page object take a `UnifiedPage`, then run the migration again |

Once done, a marker is gone, or it reads `// Escape Hatch kept: <reason>`.

## Running in a browser

A migrated test that needs nothing Browserless-only can run in a browser too. Add a browser adapter (`addPlaywright` or `addSelenium`, or the dependency by hand) and run with `-Dvaadin.testing.tool=playwright` or `selenium`:

- **Spring Boot** (`@UnifiedSpringBootTest`): the browser Testing Tools start the application from the same Spring context on a random port. Nothing else to set up.
- **Plain servlet or other** (`@UnifiedUITest`): the Unified API never starts a server. Start the application from the build (for example Failsafe with `jetty:start`) and point the tests at it with `vaadin.testing.base-url` ([test setup](spec/02-test-setup.md)).

Expect a few tests to fail on the first browser run that passed on Browserless:

- A check on the backend right after an action (`assertEquals(1, repository.count())`) holds in a browser too, because Auto-Wait waits for the round-trip, unless the view does the work in the background (`ui.access(..)` from another thread, push). Turn such a check into a retrying assertion or `page.waitUntil(..)`.
- A locator that found one component on Browserless can find two in a browser, for example a closed dialog still in the page. Add the filter that picks the intended one.

## The agent skill

The same `resolve-migration-markers` skill works through the Browserless Migration Markers: it resolves each one into Unified API code, or keeps the Escape Hatch and replaces the marker with `// Escape Hatch kept: <reason>`, and runs the tests it changed on Browserless and Playwright. Add `addAgentSkill: true` to the recipe's options, then, in Claude Code, in the migrated project:

```text
/resolve-migration-markers
```

How it is evaluated, and what it scores: [Migrating from TestBench](migrating-from-testbench.md#the-agent-skill).
