# 8. Escape Hatch

Decided in [#10](https://github.com/jcgueriaud1/unified-testing-tool/issues/10).

```java
Locator locator = name.unwrap(Locator.class);                 // fails on other Testing Tools
name.tryUnwrap(Locator.class)                                 // empty on other Testing Tools
    .ifPresent(l -> l.evaluate("e => getComputedStyle(e).color"));
WebDriver driver = page.unwrap(WebDriver.class);
```

## Types

| Testing Tool | Wrapper / Unified Element | UnifiedPage |
|---|---|---|
| Playwright | Dramafinder element (e.g. `TextFieldElement`), `Locator` | `Page` |
| Selenium | TestBench element (e.g. `TextFieldElement`), `WebElement` | `WebDriver` |
| Browserless | tester (e.g. `TextFieldTester`), `Component` | `BrowserlessUIContext` |

No compile-time dependency on any Testing Tool: only the tool a run uses must be on the classpath.

## Rules

```mermaid
flowchart TD
    U["unwrap(X.class)"] --> Q{current Testing Tool<br/>provides X?}
    Q -->|yes| R[returns X]
    Q -->|no| A{"test declares<br/>@RequiresTestingTool?"}
    A -->|yes| S[test skipped]
    A -->|no| F["test fails:<br/>message names the annotation"]
```

| Case | Use |
|---|---|
| Test needs one tool | `@RequiresTestingTool(PLAYWRIGHT)` + `unwrap` |
| TestBench test base API (`$(..)`, `executeScript`, `waitUntil`) on Selenium, as in a migrated test | `TestBenchEscapeHatch.of(page)` + `@RequiresTestingTool(SELENIUM)` ([ADR 0004](../adr/0004-test-migration-openrewrite-recipes-then-agent-skill.md)) |
| Extra check, test runs everywhere | `tryUnwrap`, no annotation |
| Server-side state (fields, beans) | `unwrap(Component.class)` + `@RequiresTestingTool(BROWSERLESS)` |
| Disable Auto-Wait, back/forward, network, tracing | the tool's own API |

Inside an Escape Hatch there are **no** Unified API guarantees: no Auto-Wait, no lazy lookup, no retrying assertions.

## Requirements on Testing Tool adapters

- Provide the types in the table, both the tool's own wrapper and the raw object.
- Without `@RequiresTestingTool`, a failed `unwrap` names the annotation to add.
