package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.internal.grid.GridSupport;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.spi.GridAccess;
import java.util.List;

/**
 * Grid wrapper. Rows are found by what they show; index access stays available. Rows and columns are
 * lazy. Row access scrolls the row into view and waits for its data (Auto-Wait), on every Testing Tool.
 * Values are rendered text; the item is reachable only through the Escape Hatch.
 */
@UnifiedComponent(Grid.class)
public interface UnifiedGrid extends UnifiedWrapper, Findable<UnifiedGrid.GridQuery> {

    interface GridQuery extends Query<UnifiedGrid, GridQuery> {}

    // rows
    default RowQuery rows() { return GridSupport.rows(this); }
    default UnifiedGridRow row(int index) { return GridSupport.row(this, index); }
    /** All rows, not only the rendered ones. Immediate; use assertThat(grid).hasRowCount(n) to wait. */
    default int getRowCount() { return element().unwrap(GridAccess.class).rowCount(); }

    // columns
    /** By header text. */
    default UnifiedGridColumn column(String header) { return GridSupport.column(this, header, false); }
    /** For columns without header text (Flow {@code Column.setKey}). */
    default UnifiedGridColumn columnWithKey(String key) { return GridSupport.column(this, key, true); }
    default List<String> getColumnHeaders() { return element().unwrap(GridAccess.class).columnHeaders(); }

    interface RowQuery {
        RowQuery withCellText(String columnHeader, String text);
        RowQuery withCellTextContaining(String columnHeader, String part);
        UnifiedGridRow single();
        List<UnifiedGridRow> all();
        boolean exists();
    }

    interface UnifiedGridRow {
        UnifiedGrid grid();
        int getIndex();
        String getCellText(String columnHeader);
    }

    interface UnifiedGridColumn {
        UnifiedGrid grid();
        /** Index among the visible columns. */
        int getIndex();
        /** All rows of this column, loading lazily. */
        List<String> getCellTexts();
    }
}
