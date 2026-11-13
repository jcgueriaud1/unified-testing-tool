---
name: resolve-migration-markers
description: Work through the Migration Markers (// TODO unified:) the TestBench Migration Recipe left, until each is resolved into Unified API code or kept as an explained Escape Hatch.
disable-model-invocation: true
---

# Resolve Migration Markers

The TestBench Migration Recipe rewrote this project's TestBench tests into Unified UI Tests. Where it could not, or where a person has to check its choice, it left a **Migration Marker**: a `// TODO unified: ...` comment above the statement. Your job is to give every marker one of two outcomes:

- **Resolved**: the code now says it with the Unified API, the test passes, and the marker is gone.
- **Kept**: the code stays as it is, and the marker becomes `// Escape Hatch kept: <reason>`, one line saying what the Unified API lacks.

A test is better the less it depends on the Escape Hatch: it then runs on every Testing Tool (Browserless, Playwright, Selenium) instead of Selenium only.

## Steps

1. **List the markers.** Search the test sources for `TODO unified:`. Write the list down: file, line, marker text. Done when the search and your list agree.
2. **Resolve or keep each marker,** using the reference below for its kind. Work one test class at a time, and run that class's tests (step 3) before the next. Done when no `TODO unified:` is left.
3. **Prove it.** For every test class you changed, run it on Browserless and on Playwright:
   `mvn test -Dtest=<Class>` and `mvn test -Dtest=<Class> -Dvaadin.testing.tool=playwright`.
   A test passes, or is skipped because it still declares `@RequiresTestingTool(TestingTool.SELENIUM)`. When one fails, fix it or put the Escape Hatch back with an explanation. Done when every changed class passes on both.
4. **Report** a table: marker, outcome (resolved or kept), what you changed.

## Reference: the Unified API

- A test gets a `UnifiedPage page`. `page.navigate(View.class)`, `page.find(UnifiedX.class).withLabel(..).single()`, `page.findComponent(Component.class)` for any component without a Wrapper.
- Wrappers are `Unified*` interfaces in `com.vaadin.testing.unified.component` (`UnifiedTextField`, `UnifiedButton`, `UnifiedGrid`, `UnifiedDialog`, ...). Read their methods in the `unified-test-api` sources before writing a call.
- Assertions retry: `import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;`, then `assertThat(wrapper).hasText(..)`, `hasValue(..)`, `isOpen()`, `isDisabled()`, ...; `assertThat(page).hasPath(..)` / `isAt(View.class)`. `page.waitUntil(() -> ...)` for anything else.
- The Escape Hatch: `TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);` gives TestBench's test base API on Selenium. When a method no longer uses `testBench`, delete that line. When a test, and the helpers it calls, no longer use it, delete `@RequiresTestingTool(TestingTool.SELENIUM)` too: the test then runs everywhere.

## Reference: marker kinds

**`TestBench Escape Hatch: <call>`.** Say the same thing with the Unified API when it can: a lookup becomes `page.find(..)`, a read becomes a Wrapper method or a retrying assertion, a script that checks the URL becomes `assertThat(page).hasPath(..)`. Keep it when it needs what only a real browser driver gives (arbitrary JavaScript, raw WebDriver, Selenium-only APIs).

**`..., a custom element: write a Wrapper for it (ADR 0003)`.** The project has its own TestBench `@Element` class. Look for an existing Wrapper first: search for `@UnifiedComponent(` naming the same Flow component (the element's `@Element` tag is the component's `@Tag`). Use it when found. Otherwise write one next to the tests: an interface with `@UnifiedComponent(TheComponent.class)`, extending `UnifiedWrapper` and `Findable<..Query>`, with `default` methods written on `element()` (`getProperty`, `getText`, `findChildren(..)`, `click()`). Translate each element method the test calls, then replace the lookup and delete the Escape Hatch.

**`was first(); single() fails when several components match`** (also `waitForFirst()`, `getCell(..)`). Run the test. When it passes, delete the marker. When it fails with "several components match", add the filter that picks the intended one (`withLabel`, `withText`, `withId`, ...), then delete the marker.

**`was withCaption(..)` / `was withLabel(..)` on a button.** Read the view: when the button shows that text, delete the marker. When it is an icon-only button, the text is its accessible name: use `withAccessibleName(..)`, then delete the marker.

**`TestBench page object: new X(this)`.** Convert the page object when every call in it has a Unified form: its TestBench field and constructor parameter become `UnifiedPage`, its `$(XElement.class)` lookups become `find(UnifiedX.class)`, its element return types become Wrappers. Then pass `page` instead of `testBench`. Keep it otherwise.

**`browser setup removed (...)`.** The Unified API's Selenium adapter chooses the browser. Move what the removed setup did to the `vaadin.testing.*` settings (`junit-platform.properties`, or system properties) when one covers it; otherwise there is nothing to carry over. Delete the marker either way and name the removed setup in your report.
