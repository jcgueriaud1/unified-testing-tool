package com.vaadin.testing.unified.internal.grid;

import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedGrid.RowQuery;
import com.vaadin.testing.unified.component.UnifiedGrid.UnifiedGridColumn;
import com.vaadin.testing.unified.component.UnifiedGrid.UnifiedGridRow;
import com.vaadin.testing.unified.internal.TestRun;
import com.vaadin.testing.unified.internal.Wrappers;
import com.vaadin.testing.unified.spi.Filter;
import com.vaadin.testing.unified.spi.GridAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Core implementation of Grid rows, columns and row queries, on {@link GridAccess}. */
public final class GridSupport {
    private GridSupport() {}

    public static RowQuery rows(UnifiedGrid grid) {
        return new Rows(grid, List.of());
    }

    public static UnifiedGridRow row(UnifiedGrid grid, int index) {
        return new Row(grid, () -> index, "row(" + index + ")");
    }

    public static UnifiedGridColumn column(UnifiedGrid grid, String headerOrKey, boolean byKey) {
        return new Column(grid, headerOrKey, byKey);
    }

    public static TestRun runOf(UnifiedGrid grid) {
        return Wrappers.lazyElementOf(grid).run();
    }

    static GridAccess access(UnifiedGrid grid) {
        return grid.element().unwrap(GridAccess.class);
    }

    static int columnIndex(GridAccess access, String header) {
        int index = access.columnHeaders().indexOf(header);
        if (index < 0) {
            throw new IllegalArgumentException("No column with header \"" + header + "\"; headers are "
                    + access.columnHeaders());
        }
        return index;
    }

    // ---------------------------------------------------------------- rows

    private record CellFilter(String column, Filter filter) {
        @Override
        public String toString() {
            return (filter.exact() ? "withCellText" : "withCellTextContaining") + "(\"" + column + "\", \""
                    + filter.value() + "\")";
        }
    }

    private record Rows(UnifiedGrid grid, List<CellFilter> filters) implements RowQuery {

        private RowQuery with(String column, String text, boolean exact) {
            List<CellFilter> copy = new ArrayList<>(filters);
            copy.add(new CellFilter(column, new Filter(Filter.Kind.TEXT, null, text, exact)));
            return new Rows(grid, List.copyOf(copy));
        }

        @Override public RowQuery withCellText(String column, String text) { return with(column, text, true); }
        @Override public RowQuery withCellTextContaining(String column, String part) { return with(column, part, false); }

        /** Matching row indexes right now. */
        List<Integer> resolve() {
            GridAccess access = access(grid);
            List<Integer> rows = null;
            for (CellFilter f : filters) {
                List<Integer> matching = access.rowsWhere(columnIndex(access, f.column()), f.filter()::matches);
                if (rows == null) {
                    rows = new ArrayList<>(matching);
                } else {
                    rows.retainAll(matching);
                }
            }
            if (rows == null) {
                rows = new ArrayList<>();
                for (int i = 0; i < access.rowCount(); i++) {
                    rows.add(i);
                }
            }
            return rows;
        }

        String describe() {
            StringBuilder sb = new StringBuilder("rows()");
            filters.forEach(f -> sb.append('.').append(f));
            return sb.toString();
        }

        @Override
        public UnifiedGridRow single() {
            int[] last = { 0 };
            return new Row(grid, () -> runOf(grid).poll(() -> {
                List<Integer> rows = resolve();
                last[0] = rows.size();
                return rows.size() == 1 ? rows.get(0) : null;
            }, () -> (last[0] == 0 ? "No row matches " : last[0] + " rows match ") + describe()
                    + (last[0] == 0 ? "" : ", expected exactly one")), describe() + ".single()");
        }

        @Override
        public List<UnifiedGridRow> all() {
            return resolve().stream().map(i -> (UnifiedGridRow) new Row(grid, () -> i, "row(" + i + ")")).toList();
        }

        @Override
        public boolean exists() {
            return !resolve().isEmpty();
        }

        @Override
        public String toString() {
            return describe();
        }
    }

    // ---------------------------------------------------------------- row and column

    /** A lazy row: its index is found again on every call. */
    public record Row(UnifiedGrid grid, Supplier<Integer> index, String description) implements UnifiedGridRow {
        @Override public int getIndex() { return index.get(); }

        @Override
        public String getCellText(String columnHeader) {
            int row = index.get();
            GridAccess access = access(grid);
            return access.cellText(row, columnIndex(access, columnHeader));
        }

        @Override public String toString() { return description; }
    }

    public record Column(UnifiedGrid grid, String headerOrKey, boolean byKey) implements UnifiedGridColumn {
        @Override
        public int getIndex() {
            GridAccess access = access(grid);
            if (!byKey) {
                return columnIndex(access, headerOrKey);
            }
            int index = access.columnIndexOfKey(headerOrKey);
            if (index < 0) {
                throw new IllegalArgumentException("No visible column with key \"" + headerOrKey + "\"");
            }
            return index;
        }

        @Override
        public List<String> getCellTexts() {
            int column = getIndex();
            return access(grid).columnTexts(column);
        }

        @Override
        public String toString() {
            return byKey ? "columnWithKey(\"" + headerOrKey + "\")" : "column(\"" + headerOrKey + "\")";
        }
    }
}
