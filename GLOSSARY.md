# Unified Testing

One API for writing Vaadin Flow tests that can run on any of Vaadin's existing testing tools.

## Language

**Unified API**:
The single Java API that Vaadin Flow tests are written against, independent of which Testing Tool runs them.
_Avoid_: Testing tool, framework, facade

**Testing Tool**:
One of the existing tools a test written against the Unified API can run on: Dramafinder (Playwright), Browserless, or TestBench Selenium.
_Avoid_: Engine, backend, driver, runner

**Escape Hatch**:
A deliberate way out of the Unified API to call a specific Testing Tool directly, for behaviour the tools don't share.
_Avoid_: Workaround, native access

**Core Set**:
The Vaadin components the Unified API spec designs in full, one per interaction pattern; every other component follows a documented pattern derived from them.
_Avoid_: Supported components, MVP components

**Auto-Wait**:
The Unified API's guarantee that an action waits for its component to be usable before acting and for the server round-trip to finish after, on every Testing Tool.
_Avoid_: Implicit wait, waitForVaadin

**Unified UI Test**:
A test written only against the Unified API, so it runs on whichever Testing Tool the test run selects.
_Avoid_: Vaadin test, browser test, integration test

**Capability**:
Something a Unified UI Test can require from the Testing Tool running it, such as a real browser; a test whose Capability is missing is skipped.
_Avoid_: Feature flag, tag

**Wrapper**:
The Unified API's object for one component of a given type, named with the `Unified` prefix, which re-finds its component on every use.
_Avoid_: Element, tester, locator, page object

**Unified Page**:
The Unified API's view of one browser tab or window showing the application: what a Unified UI Test receives, navigates and searches in.
_Avoid_: UI, window, driver, browser

**Unified Element**:
The portable, low-level handle to one component (properties, attributes, text, user actions) that every Testing Tool supports and on which Wrappers are written.
_Avoid_: Raw element, WebElement, locator

**Item Name**:
What a test calls one item of a component with several (a tab, a menu item): its visible text, or its accessible name when it shows no text, the same on every Testing Tool.
_Avoid_: Label, caption, item text

**Migration Recipe**:
An OpenRewrite recipe that rewrites a Testing Tool's existing tests into Unified UI Tests, converting what maps and leaving a Migration Marker where it can't.
_Avoid_: Converter, codemod, migration script

**Migration Marker**:
The `// TODO unified:` comment a Migration Recipe leaves on code it couldn't convert, usually kept running through the Escape Hatch.
_Avoid_: TODO, FIXME, warning

**Component Reference**:
A test's direct handle on a component's Java object, such as a field of the view, which only Browserless has; a Unified UI Test locates components instead.
_Avoid_: Field access, component instance
