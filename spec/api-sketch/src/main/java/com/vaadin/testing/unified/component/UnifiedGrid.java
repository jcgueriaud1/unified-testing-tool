package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import java.util.List;

/**
 * Grid wrapper. Rows are found by what they show; index access stays available. Rows, columns and
 * cells are lazy; cells, headers and details are Scopes. Row access scrolls the row into view and waits
 * for its data (Auto-Wait), on every Testing Tool. Values are rendered text; the item is reachable only
 * through the Escape Hatch.
 */
@UnifiedComponent(Grid.class)
public interface UnifiedGrid extends UnifiedWrapper, Findable<UnifiedGrid.GridQuery> {

    interface GridQuery extends Query<UnifiedGrid, GridQuery> {}

    // rows
    RowQuery rows();
    UnifiedGridRow row(int index);
    /** All rows, not only the rendered ones. Immediate; use assertThat(grid).hasRowCount(n) to wait. */
    int getRowCount();

    // columns
    /** By header text. */
    UnifiedGridColumn column(String header);
    /** By id, for columns without header text (Flow {@code Column.setId}); the id reaches the browser. */
    UnifiedGridColumn columnWithId(String id);
    /** By position among the visible columns; last resort. */
    UnifiedGridColumn column(int index);
    List<String> getColumnHeaders();

    // selection
    void selectAll();
    void deselectAll();

    interface RowQuery {
        RowQuery withCellText(String columnHeader, String text);
        RowQuery withCellTextContaining(String columnHeader, String part);
        RowQuery selected();
        UnifiedGridRow single();
        List<UnifiedGridRow> all();
        boolean exists();
    }

    interface UnifiedGridRow extends UnifiedWrapper {
        int getIndex();
        String getCellText(String columnHeader);
        /** Components inside the cell, e.g. an Edit button. */
        Scope cell(String columnHeader);
        void select();
        void deselect();
        boolean isSelected();
        void click();
        void doubleClick();
        void toggleDetails();
        Scope details();
    }

    interface UnifiedGridColumn extends UnifiedWrapper {
        /** Header content, e.g. a filter field. */
        Scope header();
        void sort(SortDirection direction);
        SortDirection getSortDirection();
        /** All rows of this column, loading lazily. */
        List<String> getCellTexts();
    }
}
