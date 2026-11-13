---
status: accepted
---

# Browserless test migration: the same recipes-then-skill path as TestBench

Browserless tests are converted to Unified UI Tests the way [ADR 0004](0004-test-migration-openrewrite-recipes-then-agent-skill.md) converts TestBench tests: Migration Recipes for the deterministic rewrite, then the agent skill for the Migration Markers they leave. Everything ADR 0004 decides holds here unless this ADR says otherwise.

## Decisions

- **Sources, in this order:** Browserless Test 25.x base classes and extension (`BrowserlessTest`, `SpringBrowserlessTest`, `BrowserlessExtension`); TestBench UI Unit 24.x (`UIUnitTest`, `SpringUIUnitTest`) through a package-rename step before it; then the context API (`BrowserlessApplicationContext`, `BrowserlessUIContext`).
- **One module, one skill.** A composite recipe `org.vaadin.addons.unified.rewrite.MigrateBrowserlessToUnified` in `unified-test-rewrite`, sharing the Migration Marker and skill-writing code with the TestBench recipe. `resolve-migration-markers` stays one skill, with marker kinds per Testing Tool, since a project can have both suites.
- **Component References stay behind the Escape Hatch.** Code that reaches into a view's Java fields (`test(view.nameField)`, `$view(View.class).saveButton`) is not converted by the recipe: guessing a locator from how the view builds the field could produce a test that finds the wrong component. The agent skill, which can read the view and run the test, replaces it with a locator.
- **The Escape Hatch:** `BrowserlessEscapeHatch.of(page)` (`unified-test-browserless`: the base-class API on the page's `BrowserlessUIContext`) for base-class sources, `page.unwrap(BrowserlessUIContext.class)` for context-API sources. Either comes with `@RequiresTestingTool(BROWSERLESS)` and a Migration Marker.
- **Capabilities:** a fully converted test gets no annotation and runs everywhere, as in ADR 0004, except where the recipe sees a Browserless-only pattern ([02-test-setup.md](../spec/02-test-setup.md): `@Transactional` rollback, a narrow Spring context without a web server). Such a test gets `@RequiresTestingTool(BROWSERLESS)` and a Migration Marker naming the pattern.
- **Build:** import `unified-test-bom`, add `unified-test-browserless` (and `unified-test-spring` for Spring projects), keep `browserless-test-spring`, remove direct `browserless-test*` and `vaadin-testbench-unit*` dependencies. Browser adapters only by option (`addPlaywright`, `addSelenium`, both off): without them the converted suite still runs on Browserless only, as before.
- **Lookups that run Java** (`withCondition(..)`, `withPropertyValue(..)`) and lookups of a component with no Wrapper stay behind the Escape Hatch with a Migration Marker: replacing them needs a locator or a Wrapper ([ADR 0003](0003-wrappers-are-interfaces-shipped-in-the-addon-jar.md)), which is the skill's job.
- **Setup:** `SpringBrowserlessTest` with `@SpringBootTest` becomes `@UnifiedSpringBootTest`, keeping `@MockitoBean` and `@WithMockUser`; plain `BrowserlessTest` and `BrowserlessExtension` become `@UnifiedUITest`, keeping `@ViewPackages`. The test gets the Unified Page as a parameter. Running a plain servlet suite in a browser (a started server, `vaadin.testing.base-url`) is project setup the migration guide explains, not a marker.
- **Browserless-only calls with a Unified meaning:** `roundTrip()` and `runPendingSignalsTasks()` are dropped (Auto-Wait), `fireShortcut(..)` becomes `page.pressKey(..)`, `UI.getCurrent().navigate(..)` becomes `page.navigate(..)`, `getCurrentView()` assertions become `assertThat(page).isAt(..)`. Anything else that reaches `UI`, `VaadinSession` or `MockVaadin` goes behind the Escape Hatch.
- **Backend checks after an action** (`assertEquals(1, repository.count())`) are left as they are, with no annotation: Auto-Wait makes them hold in a browser too, except for work the view starts in the background, which the skill's Playwright run finds.
- **Shared rules from ADR 0004:** `first()`-like lookups become `single()` with a marker, JUnit assertions on readable state become retrying `UnifiedAssertions`, button caption lookups follow [ADR 0001](0001-locator-filters-follow-component-type.md), `@BeforeEach` and helpers take the Unified Page, JUnit 6 is the target.

## Consequences

- A guide, `docs/migrating-from-browserless.md`, laid out like the TestBench one, also explains running a plain servlet suite in a browser.
- Options: `addPlaywright`, `addSelenium`, `includePageObjects`, `addAgentSkill`, all off. Shared base classes and helpers are converted without an option, as in ADR 0004.
- Each source style ships with a golden pair in the demo, starting with a base-class `ProfileBrowserlessTest` and its `ProfileBrowserlessMigratedTest`, run on all three Testing Tools in CI; `EditPersonBrowserlessTest` becomes the context API's golden source.

## Considered Options

- **Inferring locators from view source in the recipe:** converts more, but a wrong guess finds the wrong component, against "partial conversion, never a broken test".
- **A separate skill per Testing Tool:** simpler to write, but a project with both suites would run two skills over the same markers.
- **A marker on every backend check after an action:** safe, but buries the markers that matter under ones that are almost always fine.
- **Adding a browser adapter by default:** the point of migrating is portability, but a suite that suddenly runs in a browser it never ran in would fail in ways the recipe can't predict; that step stays deliberate.
