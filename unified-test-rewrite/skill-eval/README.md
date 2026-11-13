# Skill evaluation baseline

`baseline.json` is the score of the last accepted run of `scripts/eval-migration-skill.sh`, for the skill whose
SHA-256 it names. A new run is acceptable when no metric is worse, and, when the skill changed, at least one is
better; `--accept` then replaces this file. See [Migrating from TestBench](../../docs/migrating-from-testbench.md#evaluating-the-skill).

First baseline (2026-11-13): all 16 markers resolved, no Escape Hatch left, 11 tests passing on Browserless and on
Playwright. Known weakness of the fixtures: for `ProfilePageObjectMigratedTest` the agent reused the demo's
already converted `ProfileMigratedPage` instead of converting `ProfileTestBenchPage` itself.
