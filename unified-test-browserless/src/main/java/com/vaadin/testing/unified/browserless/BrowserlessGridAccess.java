package com.vaadin.testing.unified.browserless;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.GridAccess;
import java.util.List;
import java.util.Objects;

/** Grid access on Browserless: in memory, through GridTester; columns are the visible ones. */
final class BrowserlessGridAccess implements GridAccess {

    private final BrowserlessSession session;
    private final Grid<?> grid;
    private final GridTester<?, ?> tester;

    BrowserlessGridAccess(BrowserlessSession session, Grid<?> grid) {
        this.session = session;
        this.grid = grid;
        this.tester = (GridTester<?, ?>) (Object) session.window().test(grid);
    }

    private void roundTrip() {
        session.window().roundTrip();
    }

    private List<? extends Grid.Column<?>> columns() {
        return grid.getColumns().stream().filter(Grid.Column::isVisible).toList();
    }

    @Override
    public int rowCount() {
        return tester.size();
    }

    @Override
    public List<String> columnHeaders() {
        return columns().stream().map(this::headerText).toList();
    }

    /**
     * The column's header text: the first header row with text for it, top to bottom. Column.getHeaderText()
     * reads only one row, which is empty once a second header row (e.g. with filters) is added.
     */
    private String headerText(Grid.Column<?> column) {
        for (HeaderRow row : grid.getHeaderRows()) {
            String text = row.getCell(column).getText();
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return column.getHeaderText() == null ? "" : column.getHeaderText();
    }

    @Override
    public int columnIndexOfId(String id) {
        List<? extends Grid.Column<?>> columns = columns();
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getId().filter(id::equals).isPresent()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String cellText(int row, int column) {
        return tester.getCellText(row, column);
    }

    // ---------------------------------------------------------------- selection, clicks

    @Override public void select(int row) { tester.select(row); roundTrip(); }
    @Override public void deselect(int row) { tester.deselect(row); roundTrip(); }
    @Override public void selectAll() { tester.selectAll(); roundTrip(); }
    @Override public void deselectAll() { tester.deselectAll(); roundTrip(); }

    @Override
    public boolean isSelected(int row) {
        return grid.getSelectedItems().contains(tester.getRow(row));
    }

    /**
     * Clicks the row. In a browser, a click also toggles the row's details when the grid opens details on
     * click; that happens on the client, so GridTester doesn't do it. Simulated here.
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void clickRow(int row) {
        tester.clickRow(row);
        if (grid.isDetailsVisibleOnClick()) {
            Object item = tester.getRow(row);
            ((Grid) grid).setDetailsVisible(item, !((Grid) grid).isDetailsVisible(item));
        }
        roundTrip();
    }
    @Override public void doubleClickRow(int row) { tester.doubleClickRow(row); roundTrip(); }

    // ---------------------------------------------------------------- sorting

    @Override
    public SortDirection sortDirection(int column) {
        return tester.getSortDirection(column);
    }

    @Override
    public void sort(int column, SortDirection direction) {
        tester.sortByColumn(column, direction);
        roundTrip();
    }

    // ---------------------------------------------------------------- details

    /** What a user does: click the row (details open on click, Flow's default). */
    @Override
    public void toggleDetails(int row) {
        clickRow(row);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean isDetailsOpen(int row) {
        return ((Grid) grid).isDetailsVisible(tester.getRow(row));
    }

    // ---------------------------------------------------------------- parts

    @Override
    public UnifiedElement cell(int row, int column) {
        return new BrowserlessElement(session, tester.getCellComponent(row, column));
    }

    @Override
    public UnifiedElement header(int column) {
        Grid.Column<?> col = columns().get(column);
        List<Component> components = grid.getHeaderRows().stream()
                .map((HeaderRow headerRow) -> headerRow.getCell(col).getComponent())
                .filter(Objects::nonNull).map(c -> (Component) c).toList();
        return new BrowserlessElement(session, grid, components);
    }

    @Override
    public UnifiedElement details(int row) {
        throw new UnsupportedOperationException("Searching inside row details is not available on Browserless: "
                + "GridTester has no details API (see \"Browserless Test: close the gaps with the Unified API\"). "
                + "Toggling and checking details works; annotate the test with @RequiresBrowser to skip it here.");
    }
}
