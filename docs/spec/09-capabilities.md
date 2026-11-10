# 9. Capabilities and limitations

Decided in [#11](https://github.com/jcgueriaud1/unified-testing-tool/issues/11) and [#9](https://github.com/jcgueriaud1/unified-testing-tool/issues/9).

## Capabilities

| Annotation | Means | Missing on |
|---|---|---|
| `@RequiresBrowser` | real browser: CSS, layout, client JS, real focus and keyboard, screenshots | Browserless |
| `@RequiresTestingTool(X)` | uses a tool-specific Escape Hatch | every tool except X |

```mermaid
flowchart TD
    C{test declares a Capability<br/>the Testing Tool lacks?} -->|yes| S[skipped]
    C -->|no| U{test calls a feature<br/>the Testing Tool lacks?}
    U -->|yes| F["fails: message names<br/>the annotation to add"]
    U -->|no| R[runs]
```

Handled at **runtime**, not compile time, so tests stay written once.

## Browser-only features in the Unified API

Each needs `@RequiresBrowser`:

```java
@Test
@RequiresBrowser
void looksRight(UnifiedPage page) {
    assertThat(saveButton).hasCssValue("cursor", "pointer");
    page.screenshot(Path.of("target/profile.png"));
}
```

Network interception, tracing and browser contexts: Escape Hatch only.

### Browser-only steps in a test for every Testing Tool

A test that runs on every Testing Tool but has a browser-only step asks `page.hasRealBrowser()` instead of declaring `@RequiresBrowser`. False on Browserless, true on Playwright and Selenium:

```java
@Test
void editProfile(UnifiedPage page) {
    page.navigate(ProfileView.class);
    // ... steps and assertions, on every Testing Tool ...
    if (page.hasRealBrowser()) {
        page.screenshot(Path.of("target/profile.png"));
    }
}
```

## Keyboard

`pressKey` works on every Testing Tool, with Playwright's key names (`"Enter"`, `"Escape"`, `"Control+s"`). On Browserless keyboard and focus are simulated:

| | Browserless |
|---|---|
| `element.pressKey(k)` | the component's own handling (Escape closes a dialog), else a `keydown` on the component and each ancestor up to the UI, as bubbling would: key listeners and shortcuts (`addClickShortcut(..).listenOn(..)`) react |
| `page.pressKey(k)` | the same, from the focused component; with a dialog open, from the dialog |
| `dispatchEvent(name)` | the DOM event on the component and each ancestor |
| `page.reload()` | new view and layout instances at the same location; kept with `@PreserveOnRefresh` |
| Not simulated | client-side behaviour: typing text, Tab moving focus, browser defaults |

Tests that depend on real browser focus behaviour declare `@RequiresBrowser`.

## Limitations

| | Playwright (Dramafinder) | Selenium (TestBench) | Browserless |
|---|---|---|---|
| CSS, layout, client JS, screenshots | ✓ | ✓ | ✗ (text snapshot of the component tree) |
| Real keyboard and focus | ✓ | ✓ | simulated |
| "Visible" means | rendered | rendered | server-side `setVisible`; CSS-hidden counts as visible |
| Property values | browser | browser | server-side; equal only for synchronised properties |
| Server-side Java state | ✗ | ✗ | ✓ (Escape Hatch) |
| Network interception, tracing | ✓ (Escape Hatch) | ✗ | ✗ |
| External URLs | ✓ | ✓ | ✗ |
| Back / forward history | Escape Hatch | Escape Hatch | ✗ |

Differences in meaning are documented, not banned. Tests that depend on the browser meaning declare `@RequiresBrowser`.

## Failure evidence

| Testing Tool | Captured automatically on failure |
|---|---|
| Playwright, Selenium | full-page screenshot: `<test class>.<method>.png` |
| Browserless | location and component tree as text: `<test class>.<method>.tree.txt` |

Written to `vaadin.testing.evidence-dir` (default `target/surefire-reports/unified-evidence`, so CI uploads it with the reports), before the session closes. The failure names the files (a suppressed exception), so they show in the test report.

## Requirements on Testing Tool adapters

- Report missing Capabilities as skipped tests.
- Unsupported calls without the annotation fail with a message naming it.
- Capture failure evidence as in the table.
