package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.GridAccess;
import java.util.List;
import java.util.stream.Collectors;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.element.GridElement;

/**
 * Grid access on Playwright through Dramafinder's GridElement, which scrolls rows into view and waits for
 * lazily loaded data.
 * <p>
 * Columns are the visible ones in display order, <b>without</b> the multi-select checkbox column: Flow's
 * {@code Grid.getColumns()} (and Browserless) don't count it, so Unified column indexes are mapped to
 * browser column indexes.
 */
final class PlaywrightGridAccess implements GridAccess {

    private final PlaywrightSession session;
    private final Locator locator;
    private final GridElement grid;

    PlaywrightGridAccess(PlaywrightSession session, Locator locator) {
        this.session = session;
        this.locator = locator;
        this.grid = new GridElement(locator);
    }

    // ---------------------------------------------------------------- columns

    /** Browser column index of each Unified column: visible, display order, selection column skipped. */
    private List<Integer> domColumns() {
        @SuppressWarnings("unchecked")
        List<Number> indexes = (List<Number>) locator.evaluate("""
                g => g._getColumns().filter(c => !c.hidden).sort((a, b) => a._order - b._order)
                      .map((c, i) => [c, i])
                      .filter(([c]) => c.localName !== 'vaadin-grid-flow-selection-column')
                      .map(([, i]) => i)""");
        return indexes.stream().map(Number::intValue).collect(Collectors.toList());
    }

    private int dom(int column) {
        List<Integer> columns = domColumns();
        if (column < 0 || column >= columns.size()) {
            throw new IllegalArgumentException("No visible column at index " + column);
        }
        return columns.get(column);
    }

    @Override
    public int rowCount() {
        grid.waitForGridToStopLoading();
        return grid.getTotalRowCount();
    }

    @Override
    public List<String> columnHeaders() {
        List<String> all = grid.getHeaderCellContents();
        return domColumns().stream().map(i -> all.get(i).strip()).toList();
    }

    @Override
    public int columnIndexOfId(String id) {
        Object domIndex = locator.evaluate(
                "(g, id) => g._getColumns().filter(c => !c.hidden).sort((a, b) => a._order - b._order)"
                        + ".findIndex(c => c.id === id)",
                id);
        return domColumns().indexOf(((Number) domIndex).intValue());
    }

    @Override
    public String cellText(int row, int column) {
        return cellOf(row, column).getText().strip();
    }

    private GridElement.CellElement cellOf(int row, int column) {
        return grid.findCell(row, dom(column))
                .orElseThrow(() -> new IllegalStateException("No cell at row " + row + ", column " + column));
    }

    private GridElement.RowElement rowOf(int row) {
        return grid.findRow(row).orElseThrow(() -> new IllegalStateException("No row " + row));
    }

    /**
     * Reads a whole column a page at a time: jumps to the first row not read yet, waits for its data, then
     * reads every rendered row's cell in one script. Far faster than one scroll per row on a lazy grid.
     */
    @Override
    public List<String> columnTexts(int column) {
        int domColumn = dom(column);
        int count = rowCount();
        String[] texts = new String[count];
        int next = 0;
        while (next < count) {
            locator.evaluate("(g, i) => g._scrollToFlatIndex(i)", next);
            grid.waitForGridToStopLoading();
            @SuppressWarnings("unchecked")
            List<List<Object>> rendered = (List<List<Object>>) locator.evaluate(RENDERED_CELLS, domColumn);
            for (List<Object> pair : rendered) {
                int index = ((Number) pair.get(0)).intValue();
                if (index >= 0 && index < count && texts[index] == null) {
                    texts[index] = String.valueOf(pair.get(1)).strip();
                }
            }
            if (texts[next] == null) {
                texts[next] = cellText(next, column); // not rendered after the jump: read it the slow way
            }
            while (next < count && texts[next] != null) {
                next++;
            }
        }
        return List.of(texts);
    }

    /** [flat row index, cell text] of every rendered body row, for one browser column. */
    private static final String RENDERED_CELLS = """
            (g, column) => {
              const lastHeader = g.shadowRoot.querySelector('thead tr.last-header-row');
              const headerRows = lastHeader ? parseInt(lastHeader.getAttribute('aria-rowindex') || '0') : 0;
              return [...g.shadowRoot.querySelectorAll('tbody tr[aria-rowindex]')]
                .filter(tr => !tr.hidden && !tr.hasAttribute('loading'))
                .map(tr => {
                  const cell = tr.querySelectorAll('td')[column];
                  const slot = cell && cell.querySelector('slot');
                  const content = slot && g.querySelector(`vaadin-grid-cell-content[slot="${slot.name}"]`);
                  return [parseInt(tr.getAttribute('aria-rowindex')) - 1 - headerRows, content ? content.innerText : null];
                })
                .filter(pair => pair[1] !== null);
            }""";

    // ---------------------------------------------------------------- selection, clicks

    @Override
    public void select(int row) {
        rowOf(row).select();
        if (!isSelected(row)) {
            throw new IllegalStateException("Row " + row + " could not be selected");
        }
    }

    @Override
    public void deselect(int row) {
        rowOf(row).deselect();
    }

    @Override
    public boolean isSelected(int row) {
        return rowOf(row).isSelected();
    }

    @Override
    public void selectAll() {
        grid.checkSelectAll();
        AutoWait.waitForVaadinIdle(session.page());
    }

    @Override
    public void deselectAll() {
        grid.uncheckSelectAll();
        AutoWait.waitForVaadinIdle(session.page());
    }

    @Override
    public void clickRow(int row) {
        firstCell(row).click();
        AutoWait.waitForVaadinIdle(session.page());
    }

    @Override
    public void doubleClickRow(int row) {
        firstCell(row).getCellContentLocator().dblclick();
        AutoWait.waitForVaadinIdle(session.page());
    }

    private GridElement.CellElement firstCell(int row) {
        return rowOf(row).getCell(dom(0));
    }

    // ---------------------------------------------------------------- sorting

    @Override
    public SortDirection sortDirection(int column) {
        GridElement.HeaderCellElement header = headerCell(column);
        if (header.isSortAscending()) {
            return SortDirection.ASCENDING;
        }
        return header.isSortDescending() ? SortDirection.DESCENDING : null;
    }

    /** Dramafinder only cycles the sorter (none → ascending → descending): click until it matches. */
    @Override
    public void sort(int column, SortDirection direction) {
        for (int clicks = 0; clicks < 3 && sortDirection(column) != direction; clicks++) {
            headerCell(column).clickSort();
        }
        if (sortDirection(column) != direction) {
            throw new IllegalStateException("Column " + column + " could not be sorted " + direction);
        }
    }

    private GridElement.HeaderCellElement headerCell(int column) {
        return grid.findHeaderCell(dom(column))
                .orElseThrow(() -> new IllegalStateException("No header cell for column " + column));
    }

    // ---------------------------------------------------------------- details

    @Override
    public boolean isDetailsOpen(int row) {
        return rowOf(row).isDetailsOpen();
    }

    /**
     * What a user does: click the row (details open on click, Flow's default). Not Dramafinder's
     * openDetails(), which clicks a hard-coded second cell.
     */
    @Override
    public void toggleDetails(int row) {
        clickRow(row);
    }

    // ---------------------------------------------------------------- parts

    @Override
    public UnifiedElement cell(int row, int column) {
        return element(cellOf(row, column).getCellContentLocator());
    }

    @Override
    public UnifiedElement header(int column) {
        @SuppressWarnings("unchecked")
        List<String> slots = (List<String>) locator.evaluate("""
                (g, column) => [...g.shadowRoot.querySelectorAll('thead tr')]
                  .map(tr => tr.querySelectorAll('th, td')[column])
                  .map(cell => cell && cell.querySelector('slot'))
                  .filter(slot => slot)
                  .map(slot => slot.name)""", dom(column));
        String selector = slots.stream().map(s -> "vaadin-grid-cell-content[slot=\"" + s + "\"]")
                .collect(Collectors.joining(", "));
        return element(locator.locator(selector.isEmpty() ? "vaadin-grid-cell-content[slot='__none__']" : selector));
    }

    @Override
    public UnifiedElement details(int row) {
        return element(rowOf(row).getDetailsCell().getCellContentLocator());
    }

    private UnifiedElement element(Locator part) {
        return new PlaywrightElement(session, part, null, PlaywrightComponents.DEFAULT);
    }
}
