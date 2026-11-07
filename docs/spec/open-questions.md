# Open questions

What the spec does not decide yet. Each item is either an open ticket on the [map](https://github.com/jcgueriaud1/unified-testing-tool/issues/2) or still too unclear to ticket. Before hand-off (`spec-v1`), each is decided or marked here as deliberately left to implementation.

## Open tickets

| Question | Ticket |
|---|---|
| (none) | |

## Not yet specified

| Topic | Question |
|---|---|
| Grid extras | TreeGrid, column reordering, Grid context menu, Grid Pro editing, drag and drop ([07-grid.md](07-grid.md)) |
| Migration from TestBench | Guidance or tooling for moving existing tests |
| Multiple users and windows | Browserless `newUser().newWindow()` vs browser contexts and pages: does `UnifiedPage` support several in one test? |
| Quarkus | `@QuarkusTest` for browsers, `QuarkusBrowserlessTest` for Browserless; likely the `@UnifiedSpringBootTest` pattern |
| Authentication | Browserless `@WithMockUser` vs a real login in browsers: running a test as a given user everywhere |

## To verify during implementation

| Item | Detail |
|---|---|
| Unused TestBench on the classpath | Confirm a Browserless or Playwright run with `unified-test-selenium` present never triggers TestBench's license check ([10-packaging.md](10-packaging.md)) |

## Wording chosen while writing the spec (to confirm)

| Item | Chosen | Note |
|---|---|---|
| Base URL setting | `vaadin.testing.base-url` | Name not decided in a ticket |
| Timeout setting | `vaadin.testing.timeout` | Name not decided in a ticket |
| Core Set wrappers beyond Button, TextField, Dialog, Notification, Grid, Container | "pattern only" | Their methods follow the same pattern; not sketched |
