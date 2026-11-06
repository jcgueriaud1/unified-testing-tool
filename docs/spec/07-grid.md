# 7. Grid

**Open.** Decided by the prototype ticket [What does the Grid wrapper look like?](https://github.com/jcgueriaud1/unified-testing-tool/issues/28).

Already fixed by other decisions:

| Rule | From |
|---|---|
| `UnifiedGrid` is in the Core Set | [#6](https://github.com/jcgueriaud1/unified-testing-tool/issues/6) |
| Components inside cells are reached through the Grid wrapper on every Testing Tool (Browserless queries can't see them) | [#7](https://github.com/jcgueriaud1/unified-testing-tool/issues/7) |
| Data loading is covered by Auto-Wait | [#8](https://github.com/jcgueriaud1/unified-testing-tool/issues/8) |
| Built with the public wrapper mechanism, overrides allowed | [#18](https://github.com/jcgueriaud1/unified-testing-tool/issues/18) |

To decide: reading cells and rows, cell components, lazy loading, selection, sorting, filtering, row details, TreeGrid.
