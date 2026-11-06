# PROTOTYPE: Grid wrapper (throwaway)

For "What does the Grid wrapper look like?" (#28) on the map "Unified testing API for Vaadin" (#2).
Never merged; branch `prototype/grid-wrapper`. Type stubs only, compiled against `spec/api-sketch`
(`mvn -f ../../spec/api-sketch install -DskipTests`, then `mvn compile`).

Same scenario (users grid: filter in the Name header, Edit button in an Actions column, selection,
sorting, row details) in two shapes:

| Variant | Row access | Close to |
|---|---|---|
| `a` cell by index | `users.getCellText(0, "Email")`, `users.cell(0, "Actions")` | TestBench `getCell(row, col)`, Browserless `getCellText(row, col)` |
| `b` rows as queries | `users.rows().withCellText("Name", "Alice").single().getCellText("Email")`, plus `users.row(0)` | Dramafinder `findRowIndexesWithColumnText`, the rest of the Unified API |
