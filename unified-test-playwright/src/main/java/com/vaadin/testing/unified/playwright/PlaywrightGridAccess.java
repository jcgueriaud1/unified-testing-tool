package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Locator;
import com.vaadin.testing.unified.spi.GridAccess;
import java.util.List;
import java.util.Objects;
import org.vaadin.addons.dramafinder.element.GridElement;

/**
 * Grid access on Playwright through Dramafinder's GridElement, which scrolls rows into view and waits for
 * lazily loaded data. Columns are the visible ones.
 */
final class PlaywrightGridAccess implements GridAccess {

    private final Locator locator;
    private final GridElement grid;

    PlaywrightGridAccess(Locator locator) {
        this.locator = locator;
        this.grid = new GridElement(locator);
    }

    @Override
    public int rowCount() {
        grid.waitForGridToStopLoading();
        return grid.getTotalRowCount();
    }

    @Override
    public List<String> columnHeaders() {
        return grid.getHeaderCellContents().stream().map(String::strip).toList();
    }

    @Override
    public int columnIndexOfKey(String key) {
        // Flow sends a column's key as the web component's "path"
        Object index = locator.evaluate(
                "(g, k) => g._getColumns().filter(c => !c.hidden).findIndex(c => c.path === k || c.getAttribute('data-key') === k)",
                key);
        return ((Number) Objects.requireNonNull(index)).intValue();
    }

    /**
     * Reads a whole column a page at a time: jumps to the first row not read yet, waits for its data, then
     * reads every rendered row's cell in one script. Far faster than one scroll per row on a lazy grid.
     */
    @Override
    public List<String> columnTexts(int column) {
        int count = rowCount();
        String[] texts = new String[count];
        int next = 0;
        while (next < count) {
            locator.evaluate("(g, i) => g._scrollToFlatIndex(i)", next);
            grid.waitForGridToStopLoading();
            @SuppressWarnings("unchecked")
            List<List<Object>> rendered = (List<List<Object>>) locator.evaluate(RENDERED_CELLS, column);
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

    /** [flat row index, cell text] of every rendered body row, for one visible column. */
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

    @Override
    public String cellText(int row, int column) {
        return grid.findCell(row, column)
                .orElseThrow(() -> new IllegalStateException("No cell at row " + row + ", column " + column))
                .getText().strip();
    }
}
