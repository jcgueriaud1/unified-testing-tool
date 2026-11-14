# Skill evaluation baseline

`baseline.json` is the score of the last accepted run of `scripts/eval-migration-skill.sh`, for the skill whose
SHA-256 it names. A new run is acceptable when no metric is worse, and, when the skill changed, at least one is
better; `--accept` then replaces this file. See [Migrating from TestBench](../../docs/migrating-from-testbench.md#evaluating-the-skill).

Current baseline (2026-11-14), with the context API (#142): all 24 markers resolved across 12 fixtures, including
`EditPersonBrowserlessMigratedTest` (a context-API test whose Grid locator stayed on `page.unwrap(BrowserlessUIContext.class)`;
the agent rewrote it with `rows().withCellText(..)`); no Escape Hatch left; 16 tests passing on Browserless and on Playwright.

Previous baseline (2026-11-14), for the skill covering both Migration Recipes (#140): all 23 markers resolved across
11 fixtures, including the Browserless ones (`ProfileBrowserlessMigratedTest`, `ProfileBrowserlessLeftoversMigratedTest`
with a Component Reference and a filter running Java, `ProfileBrowserlessNarrowContextMigratedTest` with a narrow Spring
context); no Escape Hatch and no single-tool test left; 15 tests passing on Browserless and on Playwright.
`singleToolTests` (tests still requiring Selenium or Browserless) replaced `seleniumOnlyTests`.

First baseline (2026-11-13): all 16 markers resolved, no Escape Hatch left, 11 tests passing on Browserless and on
Playwright. Known weakness of the fixtures: for `ProfilePageObjectMigratedTest` the agent reused the demo's
already converted `ProfileMigratedPage` instead of converting `ProfileTestBenchPage` itself.
