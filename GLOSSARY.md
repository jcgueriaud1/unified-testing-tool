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
