---
status: proposed
---

# Dramafinder test migration: the same recipes-then-skill path, for 1.x and 2.x

Dramafinder tests are converted to Unified UI Tests the way [ADR 0004](0004-test-migration-openrewrite-recipes-then-agent-skill.md) converts TestBench tests and [ADR 0005](0005-browserless-migration-recipes.md) converts Browserless tests: Migration Recipes for the deterministic rewrite, then the agent skill for the Migration Markers they leave. Everything ADR 0004 decides holds here unless this ADR says otherwise.

## Decisions

- **Sources, in this order:** Dramafinder 2.x, then Dramafinder 1.x. Both lookup styles are converted: the static factories (`TextFieldElement.getByLabel(page, ..)`, `ButtonElement.getByText(page, ..)`, `GridElement.get(page)`) and the 2.x query API (`find(page, TextFieldElement.class).withLabel(..)`).
- **1.x needs no rename step.** 2.x renamed no packages or classes, so the same recipes handle both. What differs is lookup semantics: 1.x label and text factories match a case-insensitive substring, also match `aria-label` and placeholder, and take the first match; 2.x ones are exact and strict. The recipe reads the `dramafinder` version from the build file and treats a version it cannot resolve as 1.x.
- **1.x lookups keep matching what they matched.** A 1.x `getByLabel`/`getByText` becomes a `…Containing` filter with `single()` and a Migration Marker (`was 1.x getByLabel`); the agent skill tightens it to `withLabel`/`withText` once the test passes.
- **One module, one skill.** A composite recipe `org.vaadin.addons.unified.rewrite.MigrateDramafinderToUnified` in `unified-test-rewrite`, sharing the Migration Marker and skill-writing code with the other recipes; `resolve-migration-markers` gets a Dramafinder marker kind.
- **Setup:** `SpringPlaywrightIT` (the 2.x class, or any project class extending `AbstractBasePlaywrightIT` with `@SpringBootTest`, which is how 1.x projects copied it from a template) becomes `@UnifiedSpringBootTest`; other `AbstractBasePlaywrightIT` subclasses become `@UnifiedUITest`. The test gets the Unified Page as a parameter, `navigate(View.class)` becomes `page.navigate(..)`, and an `@EnabledIfSystemProperty` guard on the Playwright tool is dropped. A copied base class is deleted once nothing extends it, or converted as a shared helper when it has methods of its own.
- **Lookups:** strict lookups become `single()`. Explicit first-match picks (`.get(0)` on a row index list, `Locator.first()`, query API `first()`, and `NotificationElement`/`BadgeElement.getByText`, which stay substring and first-match in 2.x) become `single()` with a `was first()` marker; `get(i)`/`nth(i)` becomes `all().get(i)`. `GridElement.get(page)` becomes `find(UnifiedGrid.class).single()` without a marker: most views have one grid, and a second one fails loudly.
- **Element assertions** (`assertValue`, `assertText`, `assertOpen`, `assertEnabled`, ...) become retrying `UnifiedAssertions`; those with no Unified counterpart get a marker.
- **Grid:** `findRow(findRowIndexesWithColumnText(col, text).get(0))` becomes `rows().withCellText(col, text).single()` with a `was first()` marker, `getCell(col).getText()`/`assertText` become `getCellText(col)` and a retrying assertion, `getTotalRowCount()` becomes `getRowCount()`. Row arithmetic on raw indexes goes behind the Escape Hatch.
- **Waits:** `AutoWait` calls, `waitForGridToStopLoading` and `waitForLoadState` after an action are dropped (Auto-Wait). `page.waitForTimeout(..)`, `waitForSelector` and `waitForCondition` become `page.waitUntil(..)` where the condition is readable, and get a marker otherwise.
- **The Escape Hatch:** `page.unwrap(Page.class)`, with the existing Dramafinder element built on it, plus `@RequiresTestingTool(PLAYWRIGHT)` and a Migration Marker. No `DramafinderEscapeHatch` class: Dramafinder elements need only a `Page` or a `Locator`, so there is no base-class API to recreate.
- **Raw Playwright calls** (`page.locator(..)`, `getByRole(..)`, `evaluate(..)`, `route(..)`, tracing, `PlaywrightAssertions`) and custom `VaadinElement` subclasses stay behind the Escape Hatch. The agent skill tries to convert them: role or tag to a Wrapper, name to `withAccessibleName`/`withText`, custom elements to Wrapper interfaces ([ADR 0003](0003-wrappers-are-interfaces-shipped-in-the-addon-jar.md)). It keeps the Escape Hatch, and says why, only for calls with no Unified meaning, and removes `@RequiresTestingTool(PLAYWRIGHT)` only when the test passes on both Playwright and Browserless.
- **Capabilities:** a fully converted test gets no annotation and runs everywhere; a test where the recipe converts a browser-only call (keyboard and focus, screenshots, `getVisibleText`) gets `@RequiresBrowser`.
- **Build:** import `unified-test-bom`, add `unified-test-playwright` (which brings Dramafinder), remove the direct `dramafinder` dependency. Other adapters only by option.
- **Shared rules from ADR 0004:** `@BeforeEach` and helpers take the Unified Page, button caption lookups follow [ADR 0001](0001-locator-filters-follow-component-type.md), JUnit 6 is the target.

## Consequences

- Options: `addBrowserless`, `addSelenium`, `includePageObjects`, `addAgentSkill`, all off.
- `EditPersonDramafinderTest` is the golden source; its `EditPersonDramafinderMigratedTest` runs on all three Testing Tools in CI.
- A guide, `docs/migrating-from-dramafinder.md`, laid out like the TestBench and Browserless ones, explains the 1.x lookup markers.
- 1.x suites get more markers than 2.x suites; that is the price of never changing what a lookup finds.

## Considered Options

- **A 1.x→2.x upgrade step before the recipe:** nothing to rename, and Dramafinder's own guide shows the lookup change can only be checked at runtime.
- **Converting 1.x lookups straight to exact filters:** reads better, but turns passing tests into failing ones, against "partial conversion, never a broken test".
- **Treating an unresolvable version as 2.x:** fewer markers, but a wrong guess breaks tests; a wrong guess of 1.x only adds markers.
- **A `DramafinderEscapeHatch` class:** nothing to recreate that `unwrap` doesn't already give.
- **Adding the Browserless adapter by default:** Browserless differs most from a real browser ("visible", CSS, client JS); running there stays a deliberate step.
- **A marker on every `GridElement.get(page)`:** safe, but noise on nearly every grid test.
