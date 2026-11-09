# One test, four ways

The same test written four times: once with the Unified API and once with each Testing Tool's own API. The point is to compare syntax: how each one sets up, finds components, acts and asserts.

**Scenario** ([`PeopleView`](https://github.com/jcgueriaud1/unified-testing-tool/blob/main/unified-test-demo/src/main/java/demo/PeopleView.java)): a grid of people next to an editor form.

1. Open the view.
2. Click the row of *Alan Turing* in the grid; the form shows that person.
3. Change *Name* to *Alan M. Turing*.
4. Click *Save*.
5. Check the notification *Saved Alan M. Turing* and that the grid row shows the new name.

All four tests are real tests in `unified-test-demo` and run in CI. The code on this page is included from them, so it can't go out of date. The Unified test runs on every Testing Tool. Each native test runs only on its own tool.

## The four tests

=== "Unified API"

    Runs on Browserless, Playwright or Selenium, chosen per run with `-Dvaadin.testing.tool`.

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonUnifiedTest.java:test"
    ```

=== "Browserless Test"

    Runs in the test JVM, without a browser or HTTP server.

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonBrowserlessTest.java:test"
    ```

=== "Dramafinder (Playwright)"

    Real browser, Spring Boot server on a random port.

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonDramafinderTest.java:test"
    ```

=== "TestBench (Selenium)"

    Real browser, Spring Boot server on a random port. Needs a TestBench license. This uses `TestBenchTestCase` with a driver the test creates itself. TestBench's JUnit 5 module (`@BrowserTest`, `BrowserTestBase`) can handle the driver for you.

    ```java
    --8<-- "unified-test-demo/src/test/java/demo/EditPersonTestBenchTest.java:test"
    ```

## Step by step

| Step | Unified API | Browserless | Dramafinder | TestBench |
|---|---|---|---|---|
| Setup | `@UnifiedSpringBootTest`, `UnifiedPage` parameter | `@SpringBootTest`, `extends SpringBrowserlessTest` | `@SpringBootTest(RANDOM_PORT)`, `extends SpringPlaywrightIT` | `@SpringBootTest(RANDOM_PORT)`, `extends TestBenchTestCase`, create and quit the driver |
| Open the view | `page.navigate(PeopleView.class)` | `navigate(PeopleView.class)` | `navigate(PeopleView.class)` | `getDriver().get("http://localhost:" + port + "/people")` |
| Find the grid | `page.find(UnifiedGrid.class).single()` | `test(find(Grid.class).single())` | `GridElement.get(page)` | `$(GridElement.class).waitForFirst()` |
| Find the row | `rows().withCellText("Name", "Alan Turing").single()` | loop over `getCellText(i, 0)` | `findRow(findRowIndexesWithColumnText(0, "Alan Turing").get(0))` | `getCell("Alan Turing")`, any column |
| Click the row | `row.click()` | `clickRow(row)` | `alan.getCell("Name").click()` | `alan.click()` |
| Fill a field | `find(UnifiedTextField.class).withLabel("Name").single().setValue(…)` | `test(find(TextField.class).withLabel("Name").single()).setValue(…)` | `TextFieldElement.getByLabel(page, "Name").setValue(…)` | `$(TextFieldElement.class).withLabel("Name").single().setValue(…)` |
| Click a button | `find(UnifiedButton.class).withText("Save").single().click()` | `test(find(Button.class).withText("Save").single()).click()` | `ButtonElement.getByText(page, "Save").click()` | `$(ButtonElement.class).withText("Save").single().click()` |
| Check the notification | `assertThat(notification).hasText(…)` (retries) | `assertEquals(…, test(notification).getText())` | `NotificationElement.getByText(page, …).assertOpen()` (retries) | `assertEquals(…, $(NotificationElement.class).waitForFirst().getText())` |
| Check the grid | `assertThat(rows().withCellText("Name", "Alan M. Turing").single()).hasCellText("Email", …)` (retries) | `assertEquals(…, getCellText(row, 0))` | `alan.getCell("Name").assertText(…)` (retries) | `assertEquals(…, getCell(row, 0).getText())` |

## What differs

**Which one looks like the Unified API?** For finding components, TestBench and Browserless are the closest. All three chain filters on a query and finish with `single()`. Browserless calls it `find(…)` like the Unified API (`$(…)` is an alias). TestBench only has `$(…)`:

```java
page.find(UnifiedTextField.class).withLabel("Name").single()      // Unified API
$(TextFieldElement.class).withLabel("Name").single()              // TestBench
test(find(TextField.class).withLabel("Name").single())            // Browserless
TextFieldElement.getByLabel(page, "Name")                         // Dramafinder
```

The Unified API uses the same query style as TestBench and Browserless (`find`, `withLabel`, `withText`, `single`). Like Dramafinder, it waits on actions and its assertions retry.

**Components.** Browserless finds the Flow component (`TextField`) and wraps it in a tester (`test(…)`) to act like a user. TestBench and Dramafinder find a browser element class (`TextFieldElement`), and each tool has its own class. The Unified API has one wrapper, `UnifiedTextField`, that works on every tool.

**Grid rows.** Only the Unified API finds a row by column header and cell text, and checks the result the same way: the Unified test finds the renamed row by its new name and checks it is still Alan's (by email), without knowing the row's index. The native tests find a row index once and reuse it:

- Browserless has no search by text, so the test loops over the rows with `getCellText(row, column)`.
- Dramafinder searches one column, by index (`findRowIndexesWithColumnText(0, …)`). It then gives a `RowElement` whose cells you can reach by header text (`getCell("Name")`).
- TestBench finds a cell by its text with `getCell("Alan Turing")`, but in any column: a person called *Email* would match too. The cell gives its row index (`getRow()`); later cells are reached by row and column index.

**Waiting.** Browserless runs synchronously, so nothing needs to wait. Dramafinder waits on every action and retries its `assert…` methods. TestBench waits for Vaadin after each action, but the test has to wait for elements that appear later (`waitForFirst()`). The Unified API waits on every tool, and its `assertThat` retries.

**Server and browser.** Browserless runs the UI in the test JVM: no HTTP server, no browser, no port. Dramafinder and TestBench need a running server (`RANDOM_PORT`) and a browser. With TestBench the test creates and closes the browser itself, and navigates by URL rather than by view class.

**Changing tools.** Each native test is tied to its tool. To move a test from Browserless to Playwright, you rewrite it. The Unified test stays as it is: you only change `-Dvaadin.testing.tool`.

See also: [Appendix A. Mapping to the Testing Tools](spec/appendix-mapping.md) lists how each Unified API call maps onto the three tools.
