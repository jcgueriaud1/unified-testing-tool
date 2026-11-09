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

    Runs in the test JVM, without a browser or HTTP server. Written with the application context API and typed locators (Vaadin 25.2+), which the Browserless documentation recommends. A test extending `SpringBrowserlessTest` and implementing `Locators` gets the same locators without the context setup.

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
| Setup | `@UnifiedSpringBootTest`, `UnifiedPage` parameter | `@SpringBootTest`, a `BrowserlessApplicationContext` bean, a user and a window per test | `@SpringBootTest(RANDOM_PORT)`, `extends SpringPlaywrightIT` | `@SpringBootTest(RANDOM_PORT)`, `extends TestBenchTestCase`, create and quit the driver |
| Open the view | `page.navigate(PeopleView.class)` | `window.navigate(PeopleView.class)` | `navigate(PeopleView.class)` | `getDriver().get("http://localhost:" + port + "/people")` |
| Find the grid | `page.find(UnifiedGrid.class).single()` | `window.findGrid(Person.class)` | `GridElement.get(page)` | `$(GridElement.class).waitForFirst()` |
| Find the row | `rows().withCellText("Name", "Alan Turing").single()` | loop over `getCellText(i, 0)` | `findRow(findRowIndexesWithColumnText(0, "Alan Turing").get(0))` | `getCell("Alan Turing")`, any column |
| Click the row | `row.click()` | `clickRow(row)` | `alan.getCell("Name").click()` | `alan.click()` |
| Fill a field | `find(UnifiedTextField.class).withLabel("Name").single().setValue(…)` | `window.findTextField().withLabel("Name").setValue(…)` | `TextFieldElement.getByLabel(page, "Name").setValue(…)` | `$(TextFieldElement.class).withLabel("Name").single().setValue(…)` |
| Click a button | `find(UnifiedButton.class).withText("Save").single().click()` | `window.findButton().withText("Save").click()` | `ButtonElement.getByText(page, "Save").click()` | `$(ButtonElement.class).withText("Save").single().click()` |
| Check the notification | `assertThat(notification).hasText(…)` (retries) | `assertEquals(…, window.findNotification().getText())` | `NotificationElement.getByText(page, …).assertOpen()` (retries) | `assertEquals(…, $(NotificationElement.class).waitForFirst().getText())` |
| Check the grid | `assertThat(rows().withCellText("Name", "Alan M. Turing").single()).hasCellText("Email", …)` (retries) | `assertEquals(…, getCellText(row, 0))` | `alan.getCell("Name").assertText(…)` (retries) | `assertEquals(…, getCell(row, 0).getText())` |

## What differs

**Which one looks like the Unified API?** Browserless and TestBench. All three start from a page or window, filter by label or text, and act on the result:

```java
page.find(UnifiedTextField.class).withLabel("Name").single().setValue("…")   // Unified API
window.findTextField().withLabel("Name").setValue("…")                      // Browserless locator
$(TextFieldElement.class).withLabel("Name").single().setValue("…")          // TestBench
TextFieldElement.getByLabel(page, "Name").setValue("…")                     // Dramafinder
```

The Unified API is closest to Browserless's application context API: a page is like a Browserless window, `openWindow()` and `openAsUser(…)` are like `newWindow()` and `newUser(…)` (see [Several users and windows](#several-users-and-windows)), and both use `find` with the same filter names. The differences: a Browserless locator has one method per component (`findTextField()`), while the Unified API takes the wrapper class (`find(UnifiedTextField.class)`), and a Browserless locator picks the single match when it acts, while the Unified API says `single()`. TestBench has the same query style, but with `$(…)` instead of `find(…)`: it has no `find`. Like Dramafinder, the Unified API waits on actions and its assertions retry.

**Components.** A Browserless locator finds the Flow component (`TextField`) and acts on it like a user; with the older API, the test wraps the component in a tester (`test(find(TextField.class).single())`). TestBench and Dramafinder find a browser element class (`TextFieldElement`), and each tool has its own class. The Unified API has one wrapper, `UnifiedTextField`, that works on every tool.

**Grid rows.** Only the Unified API finds a row by column header and cell text, and checks the result the same way: the Unified test finds the renamed row by its new name and checks it is still Alan's (by email), without knowing the row's index. The native tests find a row index once and reuse it:

- Browserless has no search by text, so the test loops over the rows with `getCellText(row, column)`.
- Dramafinder searches one column, by index (`findRowIndexesWithColumnText(0, …)`). It then gives a `RowElement` whose cells you can reach by header text (`getCell("Name")`).
- TestBench finds a cell by its text with `getCell("Alan Turing")`, but in any column: a person called *Email* would match too. The cell gives its row index (`getRow()`); later cells are reached by row and column index.

**Waiting.** Browserless runs synchronously, so nothing needs to wait. Dramafinder waits on every action and retries its `assert…` methods. TestBench waits for Vaadin after each action, but the test has to wait for elements that appear later (`waitForFirst()`). The Unified API waits on every tool, and its `assertThat` retries.

**Server and browser.** Browserless runs the UI in the test JVM: no HTTP server, no browser, no port. The test opens a user and a window itself and closes the user afterwards. Dramafinder and TestBench need a running server (`RANDOM_PORT`) and a browser. With TestBench the test creates and closes the browser itself, and navigates by URL rather than by view class.

**Changing tools.** Each native test is tied to its tool. To move a test from Browserless to Playwright, you rewrite it. The Unified test stays as it is: you only change `-Dvaadin.testing.tool`.

## Several users and windows

A second window shares the session of the first, as a second browser tab would. A second user has a session of their own. This is how each tool opens them. These are API sketches, not run as tests here; the Unified API's are covered by [11. Users](spec/11-users.md) and `UsersTest`.

| | Unified API | Browserless | Dramafinder (Playwright) | TestBench (Selenium) |
|---|---|---|---|---|
| Another window, same user | `page.openWindow()` | `window.getUser().newWindow()` | `page.context().newPage()` | `getDriver().switchTo().newWindow(WindowType.WINDOW)` |
| Run the test as a user | `@WithMockUser(username = "alice", roles = "ADMIN")` | `app.newUser("alice", "ADMIN")` on a secured context | `loginAs("alice", "ADMIN")` (`SpringPlaywrightIT`, Dramafinder 2.0.1+) | log in through the application's login view |
| Another user | `page.openAsUser("bob", "USER")` | `app.newUser("bob", "USER")` on a secured context | `newPageAs("bob", "USER")` (`SpringPlaywrightIT`, Dramafinder 2.0.1+) | a new driver, then log in through the application's login view |
| Switch between them | use the other `UnifiedPage` | use the other window: no explicit switch | use the other `Page` | `switchTo().window(handle)`, or the other driver |

Browserless gives each user their own security context without logging in, and switches the Vaadin and security state automatically when the test uses another window. Dramafinder's `SpringPlaywrightIT` logs a browser context in as a test user through a test-only endpoint; another user gets a new browser context. TestBench has no test users: a second user is a second driver that logs in like a person would. The Unified API uses Spring Security's own `@WithMockUser` and, on Playwright and Selenium, logs in through its own test-only endpoint, so the same test runs as the same user on every tool.

See also: [Appendix A. Mapping to the Testing Tools](spec/appendix-mapping.md) lists how each Unified API call maps onto the three tools.
