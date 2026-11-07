# Appendix A. Mapping to the Testing Tools

How each Unified API call maps onto the three Testing Tools today. "Adapter" marks behaviour the adapter adds because the tool lacks it ([appendix-gaps.md](appendix-gaps.md)). Side-by-side test: [COMPARISON.md](https://github.com/jcgueriaud1/unified-testing-tool/blob/prototype/component-wrapper/prototypes/component-wrapper/COMPARISON.md).

## Setup and navigation

| Unified API | Browserless | Dramafinder (Playwright) | TestBench (Selenium) |
|---|---|---|---|
| `@UnifiedUITest` | `BrowserlessExtension` | adapter owns Playwright + browser | `@BrowserTest` setup, adapter-owned driver |
| `@UnifiedSpringBootTest` | `SpringBrowserlessApplicationContext.create(springCtx, …)` | `@SpringBootTest(RANDOM_PORT)` + port | same |
| `page.navigate(View.class)` | `navigate(View.class)` | `page.navigate(baseUrl + path)` + Vaadin idle (adapter) | `getDriver().get(baseUrl + path)` |
| `page.reload()` | page reload simulation | `page.reload()` | `navigate().refresh()` |
| `assertThat(page).isAt(View.class)` | `getCurrentView()` | URL vs resolved path (adapter) | URL vs resolved path (adapter) |

## Locating

| Unified API | Browserless | Dramafinder | TestBench |
|---|---|---|---|
| `find(UnifiedTextField.class)` | `find(TextField.class)` / `findTextField()` | locator on `vaadin-text-field` | `$(TextFieldElement.class)` |
| `.withLabel("Name")` | `withLabel` | `getByLabel` + exact (adapter) | `withLabel` |
| `.withAccessibleName("x")` | `withLabel` / `withAriaLabel` combined (adapter) | `getByRole(…, name)` | label or `aria-label` (adapter) |
| `.withText("Save")` | `withText` | `getByText` / `filter(hasText)` | `withText` |
| `.withTestId("x")` | `withTestId` | `getByTestId` | `withTestId` (25.4+) or `data-testid` attribute |
| `.single()` | `single()` | locator, strict | `single()`, re-resolved per call (adapter) |
| `dialog.find(…)` | `inside(dialog)` | locator chained from the dialog | `dialog.$(…)` |
| `findComponent(Rating.class)` | `find(Rating.class)` | locator on `@Tag` | `$("tag")` |

## Actions, waiting, assertions

| Unified API | Browserless | Dramafinder | TestBench |
|---|---|---|---|
| `field.setValue("x")` | tester `setValue` (as user) | `setValue` (fills, change, idle) | `setValue` |
| `button.click()` | tester `click` | `click()` + Vaadin idle (adapter) | `click()` |
| Auto-Wait before | synchronous | Playwright actionability | element-state wait (adapter) |
| Auto-Wait after | synchronous | `waitForVaadinIdle` (adapter for most actions) | `waitForVaadin` |
| `assertThat(field).hasValue("x")` | retry with `roundTrip()` + signals (adapter) | `assertValue` | retry loop (adapter) |
| `page.waitUntil(…)` | poll + flush (adapter) | poll | `waitUntil` |

## Grid

| Unified API | Browserless | Dramafinder | TestBench |
|---|---|---|---|
| `rows().withCellText(col, text)` | scan `getCellText(row, col)` | `findRowIndexesWithColumnText` | scan `getCell(row, col).getText()` |
| `row.getCellText(col)` | `getCellText(row, col)` | `getCell(..).getText()` | `getCell(row, col).getText()` |
| `row.cell(col).find(..)` | `getCellComponent(row, col)` | cell content locator | cell element `$(..)` |
| `row.select()` | `select(row)` | row checkbox / click | `select(row)` |
| `column.sort(dir)` | `sortByColumn(col, dir)` | header sorter click | header sorter click |
| row access, lazy data | in memory | `waitForGridToStopLoading` | `scrollToRow` |
| `getRowCount()` | `size()` | `getTotalRowCount()` | `getRowCount()` |

## Escape Hatch

| `unwrap(…)` | Browserless | Dramafinder | TestBench |
|---|---|---|---|
| wrapper | `TextFieldTester`, `Component` | `TextFieldElement`, `Locator` | `TextFieldElement`, `WebElement` |
| page | `BrowserlessUIContext` | `Page` | `WebDriver` |
