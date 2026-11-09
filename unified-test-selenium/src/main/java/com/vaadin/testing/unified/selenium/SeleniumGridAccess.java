package com.vaadin.testing.unified.selenium;

import com.vaadin.flow.component.grid.testbench.GridElement;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.GridAccess;
import java.util.List;
import org.openqa.selenium.WebElement;

/**
 * Grid access on Selenium: the grid's own JavaScript API to scroll and read rows (the same scripts as the
 * Playwright adapter), TestBench's GridElement for selection.
 * <p>
 * Columns are the visible ones in display order, <b>without</b> the multi-select checkbox column, as in
 * Flow's {@code Grid.getColumns()}.
 */
final class SeleniumGridAccess implements GridAccess {

    /** {@code cols}: visible columns in display order; {@code data}: the same without the selection column. */
    private static final String COLUMNS = """
            const g = arguments[0];
            const cols = g._getColumns().filter(c => !c.hidden).sort((a, b) => a._order - b._order);
            const data = cols.filter(c => c.localName !== 'vaadin-grid-flow-selection-column');
            """;

    /** Header cell contents of data column arguments[1]: its own and its column groups' (header rows). */
    private static final String HEADER_CONTENTS = COLUMNS + """
            const contents = [];
            for (let c = data[arguments[1]]; c && c !== g; c = c.parentElement) {
              const content = c._headerCell && c._headerCell._content;
              if (content) contents.push(content);
            }
            """;

    /** The rendered, loaded body row arguments[1] (flat index), or null. */
    private static final String ROW = """
            const g = arguments[0];
            const lastHeader = g.shadowRoot.querySelector('thead tr.last-header-row');
            const headerRows = lastHeader ? parseInt(lastHeader.getAttribute('aria-rowindex') || '0') : 0;
            const tr = [...g.shadowRoot.querySelectorAll('tbody tr[aria-rowindex]')]
              .find(tr => parseInt(tr.getAttribute('aria-rowindex')) - 1 - headerRows === arguments[1]
                && !tr.hidden && !tr.hasAttribute('loading'));
            """;

    private final SeleniumSession session;
    private final WebElement grid;

    SeleniumGridAccess(SeleniumSession session, WebElement grid) {
        this.session = session;
        this.grid = grid;
    }

    private Object js(String script, Object... args) {
        Object[] all = new Object[args.length + 1];
        all[0] = grid;
        System.arraycopy(args, 0, all, 1, args.length);
        return session.js(script, all);
    }

    private GridElement testBench() {
        return session.testBench(grid).wrap(GridElement.class);
    }

    // ---------------------------------------------------------------- columns

    private int dom(int column) {
        Number dom = (Number) js(COLUMNS + "const c = data[arguments[1]]; return c ? cols.indexOf(c) : -1;", column);
        if (dom.intValue() < 0) {
            throw new IllegalArgumentException("No visible column at index " + column);
        }
        return dom.intValue();
    }

    @Override
    public int rowCount() {
        return testBench().getRowCount();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> columnHeaders() {
        return (List<String>) js(COLUMNS + """
                return data.map((_, i) => {
                  for (let c = data[i]; c && c !== g; c = c.parentElement) {
                    const content = c._headerCell && c._headerCell._content;
                    const text = content ? content.innerText.trim() : '';
                    if (text) return text;
                  }
                  return '';
                });""");
    }

    @Override
    public int columnIndexOfId(String id) {
        return ((Number) js(COLUMNS + "return data.findIndex(c => c.id === arguments[1]);", id)).intValue();
    }

    // ---------------------------------------------------------------- rows and cells

    /** Scrolls the row into view and waits until it is rendered with its data. */
    private void scrollTo(int row) {
        if (row < 0 || row >= rowCount()) {
            throw new IllegalStateException("No row " + row);
        }
        long deadline = System.nanoTime() + session.timeout().toNanos();
        while (!Boolean.TRUE.equals(js(ROW + "return !!tr;", row))) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("Row " + row + " was not rendered");
            }
            js("arguments[0]._scrollToFlatIndex(arguments[1]);", row);
            session.waitForVaadin();
        }
    }

    private WebElement cellContent(int row, int column) {
        scrollTo(row);
        WebElement content = (WebElement) js(ROW + """
                const cell = tr.querySelectorAll('td')[arguments[2]];
                const slot = cell && cell.querySelector('slot');
                return slot ? g.querySelector(`vaadin-grid-cell-content[slot="${slot.name}"]`) : null;""",
                row, dom(column));
        if (content == null) {
            throw new IllegalStateException("No cell at row " + row + ", column " + column);
        }
        return content;
    }

    @Override
    public String cellText(int row, int column) {
        return ((String) session.js("return arguments[0].innerText;", cellContent(row, column))).strip();
    }

    /** Reads a whole column a page at a time, as the Playwright adapter does. */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> columnTexts(int column) {
        int domColumn = dom(column);
        int count = rowCount();
        String[] texts = new String[count];
        int next = 0;
        while (next < count) {
            js("arguments[0]._scrollToFlatIndex(arguments[1]);", next);
            session.waitForVaadin();
            List<List<Object>> rendered = (List<List<Object>>) js(RENDERED_CELLS, domColumn);
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

    /** [flat row index, cell text] of every rendered, loaded body row, for one browser column. */
    private static final String RENDERED_CELLS = """
            const g = arguments[0], column = arguments[1];
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
              .filter(pair => pair[1] !== null);""";

    // ---------------------------------------------------------------- selection, clicks

    @Override
    public void select(int row) {
        scrollTo(row);
        testBench().select(row);
        session.waitForVaadin();
        if (!isSelected(row)) {
            throw new IllegalStateException("Row " + row + " could not be selected");
        }
    }

    @Override
    public void deselect(int row) {
        scrollTo(row);
        testBench().deselect(row);
        session.waitForVaadin();
    }

    @Override
    public boolean isSelected(int row) {
        scrollTo(row);
        return Boolean.TRUE.equals(js(ROW + "return tr.hasAttribute('selected');", row));
    }

    @Override
    public void selectAll() {
        if (!selectAllChecked()) {
            testBench().clickSelectAll();
            session.waitForVaadin();
        }
    }

    @Override
    public void deselectAll() {
        if (selectAllChecked()) {
            testBench().clickSelectAll();
            session.waitForVaadin();
        }
    }

    private boolean selectAllChecked() {
        return Boolean.TRUE.equals(js(COLUMNS + """
                const selection = cols.find(c => c.localName === 'vaadin-grid-flow-selection-column');
                return !!(selection && selection.selectAll);"""));
    }

    @Override
    public void clickRow(int row) {
        new SeleniumElement(session, cellContent(row, 0), SeleniumComponents.DEFAULT).click();
    }

    @Override
    public void doubleClickRow(int row) {
        new SeleniumElement(session, cellContent(row, 0), SeleniumComponents.DEFAULT).doubleClick();
    }

    // ---------------------------------------------------------------- sorting

    private WebElement sorter(int column) {
        WebElement sorter = (WebElement) js(HEADER_CONTENTS
                + "for (const c of contents) { const s = c.querySelector('vaadin-grid-sorter'); if (s) return s; } return null;",
                column);
        if (sorter == null) {
            throw new IllegalStateException("Column " + column + " is not sortable");
        }
        return sorter;
    }

    @Override
    public SortDirection sortDirection(int column) {
        String direction = (String) session.js("return arguments[0].direction || null;", sorter(column));
        return direction == null ? null : "asc".equals(direction) ? SortDirection.ASCENDING : SortDirection.DESCENDING;
    }

    /** Clicks the sorter (none → ascending → descending) until it matches. */
    @Override
    public void sort(int column, SortDirection direction) {
        for (int clicks = 0; clicks < 3 && sortDirection(column) != direction; clicks++) {
            new SeleniumElement(session, sorter(column), SeleniumComponents.DEFAULT).click();
        }
        if (sortDirection(column) != direction) {
            throw new IllegalStateException("Column " + column + " could not be sorted " + direction);
        }
    }

    // ---------------------------------------------------------------- details

    @Override
    public boolean isDetailsOpen(int row) {
        scrollTo(row);
        return Boolean.TRUE.equals(js(ROW + "return tr.hasAttribute('details-opened');", row));
    }

    /** What a user does: click the row (details open on click, Flow's default). */
    @Override
    public void toggleDetails(int row) {
        clickRow(row);
    }

    // ---------------------------------------------------------------- parts

    @Override
    public UnifiedElement cell(int row, int column) {
        return element(cellContent(row, column));
    }

    @Override
    @SuppressWarnings("unchecked")
    public UnifiedElement header(int column) {
        List<WebElement> contents = (List<WebElement>) js(HEADER_CONTENTS + "return contents;", column);
        if (contents.isEmpty()) {
            throw new IllegalStateException("No header cell for column " + column);
        }
        return new SeleniumElement(session, contents.get(0), SeleniumComponents.DEFAULT, contents);
    }

    @Override
    public UnifiedElement details(int row) {
        scrollTo(row);
        WebElement content = (WebElement) js(ROW + """
                const cell = tr.querySelector('td[part~="details-cell"]');
                const slot = cell && cell.querySelector('slot');
                return slot ? g.querySelector(`vaadin-grid-cell-content[slot="${slot.name}"]`) : null;""", row);
        if (content == null) {
            throw new IllegalStateException("Row " + row + " has no details open");
        }
        return element(content);
    }

    private UnifiedElement element(WebElement part) {
        return new SeleniumElement(session, part, SeleniumComponents.DEFAULT);
    }
}
