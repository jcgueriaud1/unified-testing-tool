// PROTOTYPE B: "rows as queries" (user-visible, like the rest of the Unified API).
// Rows are found by what they show (cell text in a column); index access stays available.
// Rows and cells are lazy wrappers; cells, headers and details are Scopes.
package proto.b;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import java.util.List;

@UnifiedComponent(Grid.class)
public interface UnifiedGrid extends UnifiedWrapper, Findable<UnifiedGrid.GridQuery> {

    interface GridQuery extends Query<UnifiedGrid, GridQuery> {}

    // rows
    RowQuery rows();                       // all rows, filterable
    UnifiedGridRow row(int index);         // by index, when order is what the test checks
    int getRowCount();

    interface RowQuery {
        RowQuery withCellText(String columnHeader, String text);
        RowQuery withCellTextContaining(String columnHeader, String part);
        RowQuery selected();
        UnifiedGridRow single();
        List<UnifiedGridRow> all();
        boolean exists();
    }

    // columns
    UnifiedGridColumn column(String header);    // by header text; withKey for columns without a header
    UnifiedGridColumn columnWithKey(String key);
    List<String> getColumnHeaders();

    // selection
    void selectAll();
    void deselectAll();

    interface UnifiedGridRow extends UnifiedWrapper {
        int getIndex();
        String getCellText(String columnHeader);
        Scope cell(String columnHeader);         // components inside the cell
        void select();
        void deselect();
        boolean isSelected();
        void click();
        void doubleClick();
        void toggleDetails();
        Scope details();
    }

    interface UnifiedGridColumn extends UnifiedWrapper {
        Scope header();                           // filter fields in the header
        void sort(SortDirection direction);
        SortDirection getSortDirection();
        List<String> getCellTexts();              // all rows of this column (loads lazily)
    }
}
