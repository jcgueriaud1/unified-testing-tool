---
status: accepted
---

# Test migration: OpenRewrite Migration Recipes, then an agent skill for Migration Markers

Existing tests are converted to Unified UI Tests in two steps, both shipped from a new module `unified-test-rewrite` in this repo, versioned with the BOM so each release matches exactly one Unified API version:

1. **Migration Recipes** (OpenRewrite) do the deterministic rewrite: build file, setup, locators, actions, assertions, page objects and Capabilities. One composite recipe, `org.vaadin.addons.unified.rewrite.MigrateTestBenchToUnified` (options `addPlaywright`, `includePageObjects`), runs the sub-recipes, which can also run alone. Files are rewritten in place; git is the review and undo tool.
2. An **agent skill** works through the **Migration Markers** (`// TODO unified: ...`) the recipes leave: it rewrites the code into Unified calls using [appendix-mapping.md](../spec/appendix-mapping.md) and the spec, or explains why it has to stay on the Escape Hatch.

TestBench (24.x and later) comes first, then Browserless, then Dramafinder.

## Rules for the TestBench recipes

- **Partial conversion, never a broken test.** Mappable code is rewritten. Unmappable code (raw `WebElement`, `executeScript`, `getDriver()`, Selenium Grid or `@RunLocally` setup) goes through the Escape Hatch (`page.unwrap(WebDriver.class)`) with a Migration Marker. Every converted test still compiles and passes on its original Testing Tool.
- **Capabilities:** a test that got a `WebDriver` Escape Hatch gets `@RequiresTestingTool(SELENIUM)`, a test using browser-only behaviour (scripts, screenshots, focus and keyboard) gets `@RequiresBrowser`, and a fully converted test gets none and runs everywhere.
- **`first()` and `waitForFirst()`** become `single()` with a Migration Marker (`was first()`); a converted test that now fails with "several matches" needs a better filter.
- **Assertions:** recognisable JUnit assertions on `getText`, `getValue`, `isOpen` and `isEnabled` become retrying `UnifiedAssertions`, and the `waitForFirst()`/`waitUntil` they made unnecessary are dropped. Other assertions are left as they are.
- **Button `withLabel`/`withCaption`** become `withText` or `withAccessibleName` ([ADR 0001](0001-locator-filters-follow-component-type.md)).
- **Base classes and page objects:** shared `BrowserTestBase` base classes and page objects that use only mappable calls are converted. Custom `@Element` classes (`extends TestBenchElement`) are not: they stay behind the Escape Hatch with a Migration Marker, and turning them into Wrapper interfaces ([ADR 0003](0003-wrappers-are-interfaces-shipped-in-the-addon-jar.md)) is left to the agent skill.
- **JUnit 4 suites** (`TestBenchTestCase`, `@Rule`) first go through OpenRewrite's `JUnit4to5Migration`, chained by the composite recipe.
- **Build:** import `unified-test-bom`, add `unified-test-spring`, `unified-test-browserless` and `unified-test-selenium` as test dependencies (`unified-test-playwright` with `addPlaywright`), and remove direct `vaadin-testbench` dependencies, which the Selenium adapter brings in.

## Considered Options

- **Agent skill only:** handles messy code better, but isn't repeatable and is hard to review across hundreds of files.
- **IntelliJ Structural Search & Replace or a custom JavaParser tool:** no type attribution or build-file edits, or both, to write ourselves; OpenRewrite has both and runs from Maven, Gradle and Moderne.
- **Full conversion only** (leave any test with unmappable code untouched): most real suites would not move at all.
- **Generating `*UnifiedTest` copies next to the originals:** two suites to keep in sync.
- **Converting `first()` to `all().get(0)`:** keeps the old behaviour, but is unidiomatic and loses Auto-Wait on the lookup.

## Consequences

- Converted tests may retry assertions that did not retry before, and may fail on ambiguous locators that `first()` hid; both are reported as Migration Markers or test failures, not silently.
- The recipes are tested at two levels: OpenRewrite `RewriteTest` before/after cases, one per row of the mapping appendix, and a golden test in CI where `EditPersonTestBenchTest`, run through the recipe, must compile and pass on all three Testing Tools. The golden test keeps the mapping appendix honest.
- A small TestBench recipe is built now to check the mapping; recipes are published only from `spec-v1`, since the API still changes between alphas. The same module later carries API upgrade recipes between versions.
