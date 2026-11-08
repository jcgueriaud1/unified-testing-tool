package com.vaadin.testing.unified.spi;

import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.UnifiedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Adapter access to one Grid, reached by the core with {@code element.unwrap(GridAccess.class)}. Columns are
 * the visible ones, in order. Reading a row scrolls it into view and waits for its data where the Testing
 * Tool loads lazily.
 */
public interface GridAccess {

    /** All rows, not only the rendered ones. */
    int rowCount();

    List<String> columnHeaders();

    /** Index of the visible column with this id ({@code Column.setId}), or -1. */
    int columnIndexOfId(String id);

    String cellText(int row, int column);

    /** The texts of all rows in {@code column}. Adapters may do this faster than reading every cell. */
    default List<String> columnTexts(int column) {
        List<String> texts = new ArrayList<>();
        int count = rowCount();
        for (int row = 0; row < count; row++) {
            texts.add(cellText(row, column));
        }
        return texts;
    }

    // ---------------------------------------------------------------- acting (each waits for the round-trip)

    void select(int row);
    void deselect(int row);
    boolean isSelected(int row);
    void selectAll();
    void deselectAll();

    void clickRow(int row);
    void doubleClickRow(int row);

    /** Null when the column is not sorted. */
    SortDirection sortDirection(int column);
    void sort(int column, SortDirection direction);

    boolean isDetailsOpen(int row);
    void toggleDetails(int row);

    // ---------------------------------------------------------------- parts, used as Scopes

    /** The cell's content (components inside it). */
    UnifiedElement cell(int row, int column);

    /** The column's header content, in every header row (e.g. a filter field). */
    UnifiedElement header(int column);

    /** The row's details content. */
    UnifiedElement details(int row);

    /** Selected rows. Adapters may do this faster than asking every row. */
    default List<Integer> selectedRows() {
        List<Integer> rows = new ArrayList<>();
        int count = rowCount();
        for (int row = 0; row < count; row++) {
            if (isSelected(row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    /** Rows whose cell in {@code column} matches. */
    default List<Integer> rowsWhere(int column, Predicate<String> matches) {
        List<String> texts = columnTexts(column);
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < texts.size(); row++) {
            if (matches.test(texts.get(row))) {
                rows.add(row);
            }
        }
        return rows;
    }
}
