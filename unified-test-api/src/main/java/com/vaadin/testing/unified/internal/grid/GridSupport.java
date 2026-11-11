package com.vaadin.testing.unified.internal.grid;

import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedGrid.RowQuery;
import com.vaadin.testing.unified.component.UnifiedGrid.UnifiedGridColumn;
import com.vaadin.testing.unified.component.UnifiedGrid.UnifiedGridRow;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.ElementResolver;
import com.vaadin.testing.unified.internal.LazyElement;
import com.vaadin.testing.unified.internal.LazyScope;
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

    public static UnifiedGridColumn columnByHeader(UnifiedGrid grid, String header) {
        return new Column(grid, Column.By.HEADER, header, -1);
    }

    public static UnifiedGridColumn columnById(UnifiedGrid grid, String id) {
        return new Column(grid, Column.By.ID, id, -1);
    }

    public static UnifiedGridColumn columnByIndex(UnifiedGrid grid, int index) {
        return new Column(grid, Column.By.INDEX, null, index);
    }

    public static TestRun runOf(UnifiedGrid grid) {
        return Wrappers.lazyElementOf(grid).run();
    }

    /** A Scope over a part of the grid (cell, header, details), resolved again on every use. */
    static Scope scope(UnifiedGrid grid, String description, java.util.function.Function<GridAccess, UnifiedElement> part) {
        ElementResolver resolver = new ElementResolver() {
            @Override
            public List<UnifiedElement> resolve() {
                return List.of(part.apply(access(grid)));
            }

            @Override
            public String describe() {
                return description;
            }
        };
        return new LazyScope(new LazyElement(resolver, -1, runOf(grid)));
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

    /** A cell filter on one column, or on any column when {@code column} is null. */
    private record CellFilter(String column, Filter filter) {
        @Override
        public String toString() {
            return (filter.exact() ? "withCellText" : "withCellTextContaining") + "("
                    + (column == null ? "" : "\"" + column + "\", ") + "\"" + filter.value() + "\")";
        }

        List<Integer> matching(GridAccess access) {
            if (column != null) {
                return access.rowsWhere(columnIndex(access, column), filter::matches);
            }
            java.util.TreeSet<Integer> rows = new java.util.TreeSet<>();
            for (int c = 0; c < access.columnHeaders().size(); c++) {
                rows.addAll(access.rowsWhere(c, filter::matches));
            }
            return new ArrayList<>(rows);
        }
    }

    private record Rows(UnifiedGrid grid, List<CellFilter> filters, boolean selectedOnly) implements RowQuery {

        Rows(UnifiedGrid grid, List<CellFilter> filters) {
            this(grid, filters, false);
        }

        @Override
        public RowQuery selected() {
            return new Rows(grid, filters, true);
        }

        private RowQuery with(String column, String text, boolean exact) {
            List<CellFilter> copy = new ArrayList<>(filters);
            copy.add(new CellFilter(column, new Filter(Filter.Kind.TEXT, null, text, exact)));
            return new Rows(grid, List.copyOf(copy), selectedOnly);
        }

        @Override public RowQuery withCellText(String column, String text) { return with(column, text, true); }
        @Override public RowQuery withCellText(String text) { return with(null, text, true); }
        @Override public RowQuery withCellTextContaining(String column, String part) { return with(column, part, false); }

        /** Matching row indexes right now. */
        List<Integer> resolve() {
            GridAccess access = access(grid);
            List<Integer> rows = null;
            for (CellFilter f : filters) {
                List<Integer> matching = f.matching(access);
                if (rows == null) {
                    rows = new ArrayList<>(matching);
                } else {
                    rows.retainAll(matching);
                }
            }
            if (selectedOnly) {
                List<Integer> selected = access.selectedRows();
                if (rows == null) {
                    rows = new ArrayList<>(selected);
                } else {
                    rows.retainAll(selected);
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
            if (selectedOnly) {
                sb.append(".selected()");
            }
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

        @Override
        public Scope cell(String columnHeader) {
            return scope(grid, description + ".cell(\"" + columnHeader + "\")",
                    access -> access.cell(index.get(), columnIndex(access, columnHeader)));
        }

        @Override public void select() { access(grid).select(index.get()); }
        @Override public void deselect() { access(grid).deselect(index.get()); }
        @Override public boolean isSelected() { return access(grid).isSelected(index.get()); }
        @Override public void click() { access(grid).clickRow(index.get()); }
        @Override public void doubleClick() { access(grid).doubleClickRow(index.get()); }
        @Override public void toggleDetails() { access(grid).toggleDetails(index.get()); }
        @Override public boolean isDetailsOpen() { return access(grid).isDetailsOpen(index.get()); }

        @Override
        public Scope details() {
            return scope(grid, description + ".details()", access -> access.details(index.get()));
        }

        @Override public String toString() { return description; }
    }

    public record Column(UnifiedGrid grid, By by, String name, int index) implements UnifiedGridColumn {

        public enum By { HEADER, ID, INDEX }

        @Override
        public int getIndex() {
            GridAccess access = access(grid);
            return switch (by) {
                case HEADER -> columnIndex(access, name);
                case ID -> {
                    int found = access.columnIndexOfId(name);
                    if (found < 0) {
                        throw new IllegalArgumentException("No visible column with id \"" + name + "\"");
                    }
                    yield found;
                }
                case INDEX -> {
                    int columns = access.columnHeaders().size();
                    if (index < 0 || index >= columns) {
                        throw new IllegalArgumentException("No visible column at index " + index + " (" + columns + " columns)");
                    }
                    yield index;
                }
            };
        }

        @Override
        public List<String> getCellTexts() {
            int column = getIndex();
            return access(grid).columnTexts(column);
        }

        @Override
        public Scope header() {
            return scope(grid, this + ".header()", access -> access.header(getIndex()));
        }

        @Override
        public void sort(SortDirection direction) {
            access(grid).sort(getIndex(), direction);
        }

        @Override
        public SortDirection getSortDirection() {
            return access(grid).sortDirection(getIndex());
        }

        @Override
        public String toString() {
            return switch (by) {
                case HEADER -> "column(\"" + name + "\")";
                case ID -> "columnWithId(\"" + name + "\")";
                case INDEX -> "column(" + index + ")";
            };
        }
    }
}
