# Open questions

What the spec does not decide yet. Each item is either an open ticket on the [map](https://github.com/jcgueriaud1/unified-testing-tool/issues/2) or still too unclear to ticket. Before hand-off (`spec-v1`), each is decided or marked here as deliberately left to implementation.

## Open tickets

| Question | Ticket |
|---|---|
| (none) | |

## Left to implementation

Deliberately not decided in this spec.

| Topic | Guidance |
|---|---|
| Grid extras | TreeGrid, column reordering, Grid context menu, Grid Pro editing, drag and drop: same pattern as [07-grid.md](07-grid.md) |
| Quarkus | Same pattern as `@UnifiedSpringBootTest`: `@QuarkusTest` with its test port for browser Testing Tools, `QuarkusBrowserlessTest` for Browserless |
| Authentication without Spring Security | The application's real login, or the Escape Hatch ([11-users.md](11-users.md)) |
| Migration from TestBench | Decided in [ADR 0004](../adr/0004-test-migration-openrewrite-recipes-then-agent-skill.md): Migration Recipes, then an agent skill for Migration Markers |

## To verify during implementation

| Item | Detail |
|---|---|
| Unused TestBench on the classpath | Confirm a Browserless or Playwright run with `unified-test-selenium` present never triggers TestBench's license check ([10-packaging.md](10-packaging.md)) |

## Confirmed after writing

| Item | Decision |
|---|---|
| Base URL setting | `vaadin.testing.base-url` |
| Timeout setting | `vaadin.testing.timeout` |
| `…Containing` filters | Case-insensitive substring match, as in Dramafinder 2.0 (found while implementing) |

## Pattern only

Core Set wrappers beyond Button, TextField, Dialog, Notification, Grid and Container follow the same pattern; they are not sketched.
