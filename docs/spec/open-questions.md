# Open questions

What the spec does not decide yet. Each item is either an open ticket on the [map](https://github.com/jcgueriaud1/unified-testing-tool/issues/2) or still too unclear to ticket. Before hand-off (`spec-v1`), each is decided or marked here as deliberately left to implementation.

## Open tickets

| Question | Ticket |
|---|---|
| Grid wrapper | [What does the Grid wrapper look like?](https://github.com/jcgueriaud1/unified-testing-tool/issues/28) |
| Modules and artifacts | [What are the Unified API's modules and artifacts?](https://github.com/jcgueriaud1/unified-testing-tool/issues/29) |

## Not yet specified

| Topic | Question |
|---|---|
| Licensing | TestBench is commercial: how are the Unified API and its adapters licensed? (depends on packaging) |
| Migration from TestBench | Guidance or tooling for moving existing tests |
| Multiple users and windows | Browserless `newUser().newWindow()` vs browser contexts and pages: does `UnifiedPage` support several in one test? |
| Quarkus | `@QuarkusTest` for browsers, `QuarkusBrowserlessTest` for Browserless; likely the `@UnifiedSpringBootTest` pattern |
| Authentication | Browserless `@WithMockUser` vs a real login in browsers: running a test as a given user everywhere |

## Wording chosen while writing the spec (to confirm)

| Item | Chosen | Note |
|---|---|---|
| Base URL setting | `vaadin.testing.base-url` | Name not decided in a ticket |
| Timeout setting | `vaadin.testing.timeout` | Name not decided in a ticket |
| Core Set wrappers beyond Button, TextField, Dialog, Notification, Container | "pattern only" | Their methods follow the same pattern; not sketched |
