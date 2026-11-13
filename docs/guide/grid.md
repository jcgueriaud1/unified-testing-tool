# Grid

Rows are found by **what they show**, like any other component. Spec: [7. Grid](../spec/07-grid.md).

```java
UnifiedGrid users = page.find(UnifiedGrid.class).withTestId("users").single();
UnifiedGrid.UnifiedGridRow alice = users.rows().withCellText("Name", "Alice").single();
```

## Finding rows

`rows()` starts a row query, with the same terminals as a component query:

| Filter | Matches |
|---|---|
| `withCellText("Name", "Alice")` | the cell under the column headed *Name* is *Alice* |
| `withCellText("Alice")` | any cell is *Alice*; reads every column, so slower on large lazy Grids |
| `withCellTextContaining("Email", "@example")` | part of the cell text, ignoring case |
| `selected()` | selected rows |

```java
--8<-- "unified-test-demo/src/test/java/demo/GridTest.java:helpers"

--8<-- "unified-test-demo/src/test/java/demo/GridTest.java:findsARowByWhatItShows"
```

Row access scrolls the row into view and waits for its data, so a row far down a lazy Grid is found like the first one.

Filters add up:

```java
--8<-- "unified-test-demo/src/test/java/demo/GridTest.java:combinesCellFilters"
```

`row(i)` reaches a row by index, for tests where the order is the point (after sorting, for example).

## Reading and checking rows

```java
assertThat(alice).hasCellText("Email", "alice@example.com").hasCellText("Role", "Admin");
String email = alice.getCellText("Email");
String first = users.row(0).getCellText(0);     // by column index, for columns without a header
int index = alice.getIndex();
assertThat(users).hasRowCount(120);             // all rows, not only the rendered ones
assertThat(archived).isEmpty();
```

## Components in cells

`row.cell("Actions")` is a [Scope](finding-components.md#scopes):

```java
--8<-- "unified-test-demo/src/test/java/demo/GridInteractionTest.java:helpers"

--8<-- "unified-test-demo/src/test/java/demo/GridInteractionTest.java:clicksAComponentInACell"
```

## Columns

Find a column by its header text; `columnWithId("owner")` (`Column.setId`) or `column(1)` for a column without header text.

```java
users.column("Name").sort(SortDirection.ASCENDING);
assertThat(users.column("Name")).isSortedAscending().hasCellTexts("Alice", "Bob", "Carol");
List<String> headers = users.getColumnHeaders();
```

`column.header()` is a Scope, for filter fields in the header:

```java
--8<-- "unified-test-demo/src/test/java/demo/GridInteractionTest.java:filtersThroughAHeaderField"
```

A column's key (`Column.setKey`) stays on the server, so no browser can find a column by it.

## Selection, clicks and details

```java
--8<-- "unified-test-demo/src/test/java/demo/GridInteractionTest.java:selectsRows"
```

```java
users.row(1).click();            // as a user clicking the row (not a button in it)
users.row(4).doubleClick();

row.toggleDetails();             // clicks the row, as a user does
assertThat(row).hasDetailsOpen();
row.details().find(UnifiedButton.class).withText("Reset password").single().click();   // browser only for now
```

The multi-select checkbox column is not a column: column indexes count only the application's columns.

## The item behind a row

A test sees what the user sees: rendered text. To read the Java item behind a row, use the [Escape Hatch](tool-specific.md) on Browserless.
