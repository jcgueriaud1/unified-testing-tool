// PROTOTYPE A: "cell by index" (closest to TestBench getCell(row, col) and Browserless getCellText(row, col)).
// Rows are addressed by index; columns by header text. Small surface, but tests depend on row order.
package proto.a;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;

@UnifiedComponent(Grid.class)
public interface UnifiedGrid extends UnifiedWrapper, Findable<UnifiedGrid.GridQuery> {

    interface GridQuery extends Query<UnifiedGrid, GridQuery> {}

    int getRowCount();                                  // all rows (data provider size)
    String getCellText(int row, String columnHeader);   // scrolls the row into view, Auto-Wait for data
    Scope cell(int row, String columnHeader);           // components inside the cell

    void select(int row);
    void deselect(int row);
    void selectAll();
    boolean isSelected(int row);

    void sort(String columnHeader);                     // click the sorter: none -> asc -> desc
    Scope header(String columnHeader);                  // filter fields in the header
    void toggleDetails(int row);
    Scope details(int row);
}
