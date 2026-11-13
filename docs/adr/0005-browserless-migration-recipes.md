---
status: proposed
---

# Browserless test migration: the same recipes-then-skill path as TestBench

Browserless tests are converted to Unified UI Tests the way [ADR 0004](0004-test-migration-openrewrite-recipes-then-agent-skill.md) converts TestBench tests: Migration Recipes for the deterministic rewrite, then the agent skill for the Migration Markers they leave. Everything ADR 0004 decides holds here unless this ADR says otherwise.

## Decisions

- **Sources, in this order:** Browserless Test 25.x base classes and extension (`BrowserlessTest`, `SpringBrowserlessTest`, `BrowserlessExtension`); TestBench UI Unit 24.x (`UIUnitTest`, `SpringUIUnitTest`) through a package-rename step before it; then the context API (`BrowserlessApplicationContext`, `BrowserlessUIContext`).
- **One module, one skill.** A composite recipe `org.vaadin.addons.unified.rewrite.MigrateBrowserlessToUnified` in `unified-test-rewrite`, sharing the Migration Marker and skill-writing code with the TestBench recipe. `resolve-migration-markers` stays one skill, with marker kinds per Testing Tool, since a project can have both suites.
- **Component references stay behind the Escape Hatch.** Code that reaches into a view's Java fields (`test(view.nameField)`, `$view(View.class).saveButton`) is not converted by the recipe: guessing a locator from how the view builds the field could produce a test that finds the wrong component. The agent skill, which can read the view and run the test, replaces it with a locator.
- **The Escape Hatch:** `BrowserlessEscapeHatch.of(page)` (`unified-test-browserless`: the base-class API on the page's `BrowserlessUIContext`) for base-class sources, `page.unwrap(BrowserlessUIContext.class)` for context-API sources. Either comes with `@RequiresTestingTool(BROWSERLESS)` and a Migration Marker.
- **Capabilities:** a fully converted test gets no annotation and runs everywhere, as in ADR 0004, except where the recipe sees a Browserless-only pattern ([02-test-setup.md](../spec/02-test-setup.md): `@Transactional` rollback, a narrow Spring context without a web server). Such a test gets `@RequiresTestingTool(BROWSERLESS)` and a Migration Marker naming the pattern.
- **Build:** import `unified-test-bom`, add `unified-test-browserless` (and `unified-test-spring` for Spring projects), keep `browserless-test-spring`, remove direct `browserless-test*` and `vaadin-testbench-unit*` dependencies. Browser adapters only by option (`addPlaywright`, `addSelenium`, both off): without them the converted suite still runs on Browserless only, as before.
- **Shared rules from ADR 0004:** `first()`-like lookups become `single()` with a marker, JUnit assertions on readable state become retrying `UnifiedAssertions`, button caption lookups follow [ADR 0001](0001-locator-filters-follow-component-type.md), `@BeforeEach` and helpers take the Unified Page, JUnit 6 is the target.

## Considered Options

- **Inferring locators from view source in the recipe:** converts more, but a wrong guess finds the wrong component, against "partial conversion, never a broken test".
- **A separate skill per Testing Tool:** simpler to write, but a project with both suites would run two skills over the same markers.
- **Adding a browser adapter by default:** the point of migrating is portability, but a suite that suddenly runs in a browser it never ran in would fail in ways the recipe can't predict; that step stays deliberate.
