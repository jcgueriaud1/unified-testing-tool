package com.vaadin.testing.unified.spi;

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

    /** Index of the visible column with this key ({@code Column.setKey}), or -1. */
    int columnIndexOfKey(String key);

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
