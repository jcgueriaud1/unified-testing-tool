# Skill evaluation baseline

`baseline.json` is the score of the last accepted run of `scripts/eval-migration-skill.sh`, for the skill whose
SHA-256 it names: the totals, and the score of each fixture (a migrated demo test that still had Migration Markers).
A new run is compared with it fixture by fixture, over the fixtures both have, so a fixture the recipes no longer
leave markers in drops out ("no longer needs the skill") instead of making the run look worse, and a new fixture is
reported and compared against nothing. Markers resolved and untouched are compared as rates of the fixture's markers;
Escape Hatches, single-tool tests and tests passed or failed on each Testing Tool as counts. A run is acceptable when
no compared metric is worse, and, when the skill changed, at least one is better, unless the baseline has nothing
left to improve (every compared fixture with all its markers resolved, no Escape Hatch, no single-tool test, no failing
test); `--accept` then replaces this file.
A baseline with totals only (before #153) compares with nothing: the first run in this format is acceptable.
See [Migrating from TestBench](../../docs/migrating-from-testbench.md#evaluating-the-skill).

Current baseline (2026-11-14), the first with per-fixture scores (#153): all 24 markers resolved across 12 fixtures,
including the new "was a scan of column" marker in `EditPersonBrowserlessMigratedTest` (#154), which the skill doesn't
list yet; no Escape Hatch and no single-tool test left; 16 tests passing on Browserless and on Playwright.

Previous baseline (2026-11-14), totals only, with the context API (#142): all 24 markers resolved across 12 fixtures, including
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
