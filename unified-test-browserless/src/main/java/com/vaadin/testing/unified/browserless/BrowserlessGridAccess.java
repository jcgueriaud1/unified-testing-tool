package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.testing.unified.spi.GridAccess;
import java.util.List;

/** Grid access on Browserless: in memory, through GridTester; columns are the visible ones. */
final class BrowserlessGridAccess implements GridAccess {

    private final Grid<?> grid;
    private final GridTester<?, ?> tester;

    BrowserlessGridAccess(BrowserlessUIContext window, Grid<?> grid) {
        this.grid = grid;
        this.tester = (GridTester<?, ?>) (Object) window.test(grid);
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
        return columns().stream().map(c -> c.getHeaderText() == null ? "" : c.getHeaderText()).toList();
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
}
