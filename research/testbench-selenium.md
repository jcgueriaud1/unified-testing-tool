# What TestBench Selenium offers (research for #5)

Question: what does **TestBench Selenium** (TestBench element classes over WebDriver) offer as a **Testing Tool** that the **Unified API** would sit on top of? (Vocabulary per `GLOSSARY.md`.)

Researched 2026-11-05 against primary sources, pinned to these revisions:

- `vaadin/testbench` `main` @ [`1ff7a49`](https://github.com/vaadin/testbench/tree/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34) (version `25.4-SNAPSHOT`), abbreviated **TB** below.
- `vaadin/flow-components` `main` @ [`75ef566`](https://github.com/vaadin/flow-components/tree/75ef56618e3bdb6d4f277dc8ed93a6bef32a24da) (`25.4-SNAPSHOT`), abbreviated **FC**.
- `vaadin/docs` `main` @ [`2b8bbb8`](https://github.com/vaadin/docs/tree/2b8bbb8bd3cae348e6b6dcf4642fc2651058e267), the source of vaadin.com/docs. "Docs: e2e/x" means `articles/flow/testing/end-to-end/x.adoc`, published at `https://vaadin.com/docs/latest/flow/testing/end-to-end/x`.
- Maven Central metadata under `https://repo1.maven.org/maven2/com/vaadin/`.

Latest release on Central: **25.3.x**. Core `vaadin-testbench-core-junit6` is at 25.3.2 and the aggregator `vaadin-testbench-junit6` at 25.3.5. On `main`, 25.4 is in alpha (tags `25.4.0-alpha1`/`-alpha2`).

## TL;DR

TestBench Selenium is a commercial, Vaadin-owned layer on Selenium 4 (`selenium.version` 4.52.0, Java 21 baseline). It adds three things on top of plain WebDriver:

1. Typed `*Element` page objects for every Vaadin component (~98 classes in 48 `*-testbench` modules in FC) plus HTML elements.
2. A tag-based `$(XElement.class)` `ElementQuery` builder that pierces shadow DOM.
3. Implicit **waitForVaadin**: before most WebDriver commands it polls until no Flow client has a server round-trip in flight.

It runs under JUnit 4, JUnit 5, and JUnit 6 (Jupiter). JUnit 6 has its own artifact since Vaadin 25.0. TestBench does **not** start the application: the test calls `getDriver().get(url)` against a server that is already running. The test base classes also give full access to the underlying Selenium `WebDriver`.

## Component API

- **Where wrappers live.** Each Vaadin component ships a `vaadin-<name>-testbench` module in FC with a `*Element` class. At `75ef566` there are **48 such modules containing 98 `*Element` classes**. The modules cover Accordion, AppLayout, Avatar, Badge, Board, Breadcrumbs, Button, Card, Charts, Checkbox, ComboBox, ConfirmDialog, ContextMenu, Crud, CustomField, Dashboard, DatePicker, DateTimePicker, Details, Dialog, FormLayout, Grid, GridPro, Icon, ListBox, Login, Map, Markdown, MasterDetailLayout, MenuBar, Messages, Notification, Horizontal/VerticalLayout, PdfViewer, Popover, ProgressBar, RadioButton, RichTextEditor, Select, SideNav, Slider, SplitLayout, Spreadsheet, Tabs/TabSheet, TextField/TextArea/Password/Email/Integer/Number/BigDecimal fields, TimePicker, Upload, and VirtualList. (Source: FC file tree, `*/vaadin-*-testbench/src/main/java/**/*Element.java`.)
- **HTML elements.** `DivElement`, `SpanElement`, and similar come from `com.vaadin:flow-html-components-testbench` (Apache-2.0, Central). That artifact is pulled in by the aggregator [`vaadin-testbench-junit6:25.0.0` POM](https://repo1.maven.org/maven2/com/vaadin/vaadin-testbench-junit6/25.0.0/vaadin-testbench-junit6-25.0.0.pom), which depends on `vaadin-testbench-core-junit6`, `flow-html-components-testbench`, and every `vaadin-*-testbench` module.
- **Docs claim:** "`Element` classes are provided for all Vaadin components" (Docs: e2e/creating-tests).
- **Shape.** Each class `extends TestBenchElement`, carries `@Element("vaadin-xxx")` (the tag), and mixes in small capability interfaces from TB `vaadin-testbench-shared`: `HasLabel`, `HasPlaceholder`, `HasHelper`, `HasValidation` (`isInvalid`, `isRequired`, `getErrorMessage`), `HasClearButton`, `HasSelectByText`, `HasStringValueProperty` (`getValue`/`setValue`/`clear`), and `HasLabelAsText`. Values are **strings**, read from DOM properties (for example `HasLabel.getLabel()` = `getPropertyString("label")`). Examples:
  - `ButtonElement`: only `getText()` on top of `TestBenchElement` ([FC ButtonElement](https://github.com/vaadin/flow-components/blob/75ef56618e3bdb6d4f277dc8ed93a6bef32a24da/vaadin-button-flow-parent/vaadin-button-testbench/src/main/java/com/vaadin/flow/component/button/testbench/ButtonElement.java)).
  - `TextFieldElement.setValue(String)` sets the inner `<input>` value and fires **synthetic** `input`, `change`, and `focusout` events. `sendKeys` is offered for real typing ([FC TextFieldElement](https://github.com/vaadin/flow-components/blob/75ef56618e3bdb6d4f277dc8ed93a6bef32a24da/vaadin-text-field-flow-parent/vaadin-text-field-testbench/src/main/java/com/vaadin/flow/component/textfield/testbench/TextFieldElement.java)).
  - `ComboBoxElement`: `selectByText`, `getSelectedText`, `openPopup`/`closePopup`/`isPopupOpen`, `getOptions`, `setFilter`/`getFilter`, `waitForLoadingFinished`.
  - `DatePickerElement`: `setDate(LocalDate)`/`getDate()`, `setInputValue`, `open`/`close`, and overlay calendar sub-elements. This is one of the few typed (non-String) values.
  - `GridElement`: `getRowCount`, `getCell(row, col)`/`getCell(String)`, `getRow(s)`, `getColumn(headerText)`, header/footer cells, `select`/`deselect(row)`, `scrollToRow`, `getVisibleCellContents`/`getAllCellContents`, `getEmptyStateContent`.
  - `NotificationElement` and `DialogElement`: `isOpen()`, `getText()`/`getHeaderTitle()`.
- **Generic `TestBenchElement` API** ([TB TestBenchElement](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-shared/src/main/java/com/vaadin/testbench/TestBenchElement.java)):
  - Interaction: `click` (JS `click()` after `focus()`, falling back to WebDriver click), `click(x,y,Keys...)`, `doubleClick`, `contextClick`, `hover`, `sendKeys`, `clear`, `focus`, `scrollIntoView`.
  - State: `isEnabled`, `isReadOnly`, `isFocused`, `isDisplayed`, `getText`, `getClassNames`/`hasClassName`.
  - DOM access: `get/setProperty*`, `getAttribute`/`getDomProperty`, `callFunction(name, args)`, `dispatchEvent`, `getShadowRoot`, `getParent`, `getChildren`.
  - Other: `wrap(Class)`, `compareScreen(...)`, `getScreenshotAs`.

## Locating

- **Entry point.** `$(XElement.class)` or `$("tag-name")` on the test case (page scope) or on any element (scoped to that element, including its shadow root) ([TB HasElementQuery](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-shared/src/main/java/com/vaadin/testbench/HasElementQuery.java); Docs: e2e/creating-tests). The element class's `@Element` tag selects the candidates. **Locating is by component class/tag, not by Java component instance.**
- **Filters on `ElementQuery`** ([TB ElementQuery](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-shared/src/main/java/com/vaadin/testbench/ElementQuery.java)):
  - Attributes: `withId`, `withAttribute`/`withoutAttribute` (with `…Containing`, `…ContainingWord` variants), `hasAttribute`, `withClassName`/`withoutClassName`, `withTheme`/`withoutTheme`.
  - Captions and text: `withLabel`/`withLabelContaining`, `withPlaceholder…`, `withCaption…` (label, then placeholder, then text, with an optional `BiPredicate` comparison), `withText`/`withTextContaining`.
  - Arbitrary: `withPropertyValue(getter, value)`, `withCondition(Predicate)`.
  - `withTestId(String)` (the `data-testid` set by `Component.setTestId`) and the terminal `testId(String)` are marked `@since 25.4`, so they are **not yet in a stable release**.
  - Scope: `context(SearchContext)` and `onPage()`.
- **Terminals:** `id(String)`, `single()`, `waitForSingle([timeout])`, `get(n)`, `last()`, `exists()`, `all()`. `first()` and `waitForFirst()` are **`@Deprecated(since = "10.0", forRemoval = true)`** in favour of `single()`/`waitForSingle()`, which assert exactly one match. The docs pages still show `first()`.
- **Mechanism.** Queries run as a JavaScript `querySelectorAll` in the light DOM plus the context's `shadowRoot` (ElementQuery `executeSearchScript`). Caption/label/property filters are evaluated in Java over the candidates.
- **Raw Selenium** `findElement(By…)` (CSS, XPath, id) is available on the test case and on elements.
- **No ARIA role or accessible-name locator exists.** Neither `ElementQuery` nor the `Has*` mixins mention `aria`/`role` (grep at `1ff7a49`). Label matching uses the component's `label` **property**, not the computed accessible name.
- **Docs guidance:** set ids on components and avoid matching on DOM structure or user-visible strings (Docs: e2e/reliable-tests).

## Waiting

- **Implicit waitForVaadin.** The `WebDriver` is wrapped in a `TestBenchDriverProxy`. Its `DriverInvocationHandler` calls `waitForVaadin()` before every driver command except a short list (`get`, `close`, `manage`, …). `TestBenchElement` calls it before most interactions and reads.
  - It polls a script: return false while `document.readyState != 'complete'`, while the dev server is not loaded, or while any `window.Vaadin.Flow.clients[*].isActive()`. It has a **hard-coded 40 s ceiling** and ends silently on timeout.
  - Sources: [TB TestBenchCommandExecutor](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-shared/src/main/java/com/vaadin/testbench/commands/TestBenchCommandExecutor.java), [DriverInvocationHandler](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-shared/src/main/java/com/vaadin/testbench/DriverInvocationHandler.java), Docs: e2e/advanced-concepts "Waiting for Vaadin".
- **Toggles:** `testBench().disableWaitForVaadin()`, `enableWaitForVaadin()`, and an explicit `waitForVaadin()`.
- **What it does not cover.** waitForVaadin waits for the **Flow client/server round-trip** only. It does **not** wait for an element to appear, become visible, or become enabled, and it does not wait for client-side animations or lazily rendered overlays. `single()`/`get()` fail immediately with `NoSuchElementException`.
- **Explicit waits:**
  - `waitForSingle(timeout)` (default 10 s).
  - `waitUntil(ExpectedCondition[, seconds])` (default 10 s; Selenium `WebDriverWait`).
  - In the JUnit 5/6 base class: `waitUntilNot`, `waitForElementPresent`/`NotPresent`/`Visible`.
  - Component-specific waits such as `ComboBoxElement.waitForLoadingFinished`.
  - Docs tell users to use these instead of `Thread.sleep` (Docs: e2e/reliable-tests, e2e/advanced-concepts).
- **Auto scroll-into-view** before interactions; can be turned off with `setAutoScrollIntoView(false)` (Docs: e2e/advanced-concepts "Scrolling").
- **Execution model.** The test runs in a separate JVM thread from the server, against a real browser. There is no synchronous server-side execution.

## JUnit support

- **JUnit 4.** `vaadin-testbench-core` depends on `junit:junit:4.13.2`. Tests extend `TestBenchTestCase` (or `ParallelTest`); `RetryRule` is a JUnit 4 `@Rule`. Under the JUnit 6 platform these tests need `junit-vintage-engine` (Docs: e2e/getting-started NOTE).
- **JUnit 5.** `vaadin-testbench-core-junit5` (since TestBench 9 / Vaadin 24) sets `junit.version` 5.14.0.
- **JUnit 6 (Jupiter): yes.** `vaadin-testbench-core-junit6` sets `junit.version` 6.0.0 and is built from the **same sources** as the junit5 module through `build-helper-maven-plugin`, so the API is identical and only the transitive JUnit versions differ ([TB core-junit6 pom](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-core-junit6/pom.xml)).
  - Released on Central from **10.0.0** (that is, Vaadin 25.0) through 25.3.2.
  - Docs: "JUnit 6 since Vaadin 25.0 … `vaadin-testbench-junit6` (or `vaadin-testbench-junit5`)" and "Starting with Vaadin 25 and Spring Boot 4, the default test framework is JUnit 6" (Docs: e2e/getting-started).
- **Jupiter API.** Extend `BrowserTestBase`, which is `@Execution(CONCURRENT)` and registers `ScreenshotOnFailureExtension`. Annotate methods with **`@BrowserTest`**, which is a `@TestTemplate` with `MultipleBrowsersExtension`; a plain `@Test` is not enough "since TestBench 9". `ParameterizedBrowserTest` is also available, and `BrowserTestInfo` can be injected (driver, capabilities, hub host, local browser) ([TB core-junit5](https://github.com/vaadin/testbench/tree/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-core-junit5/src/main/java/com/vaadin/testbench); Docs: e2e/advanced-concepts).
- **Retries** under Jupiter use Failsafe `rerunFailingTestsCount` or the Gradle retry plugin, not `RetryRule` (Docs: e2e/reliable-tests).

## App lifecycle

- **TestBench does not start or embed the application.** The test opens a URL with `getDriver().get("http://localhost:8080/")`, and "the server hosting your application needs to be running at the given URL before you launch your test" (Docs: e2e/getting-started).
- **Documented ways to get a server:**
  - `mvn spring-boot:run` or `mvn jetty:run` in another terminal.
  - Binding the Jetty Maven plugin's `start`/`stop` to `pre-`/`post-integration-test` and running `*IT` tests with Failsafe (Docs: e2e/maven, e2e/getting-started; example repo `vaadin/testbench-demo`).
  - Any external URL. CI guidance covers `IPAddress.findSiteLocalAddress()` for remote browsers (Docs: e2e/ci-server).
- **`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@LocalServerPort`** is documented for plain Selenium and Playwright (Docs: `flow/testing/selenium.adoc`, `playwright.adoc`), not on the TestBench pages. Combining it with `BrowserTestBase` should work because TestBench is URL-driven, but that is **unverified** (no first-party TestBench example found).
- **Browser/driver lifecycle.**
  - JUnit 5/6: `@BrowserTest` creates the driver (local Chrome by default; `@RunLocally`, `@RunOnHub`, `@BrowserConfiguration`, and `com.vaadin.testbench.Parameters.*` system properties such as `runLocally`, `headless`, `hubHostname`, `gridBrowsers`, `testsInParallel`).
  - JUnit 4: the user calls `setDriver(new ChromeDriver())` and quits it.
  - Drivers come from Selenium Manager (Docs: e2e/maven "WebDriver Management", e2e/multiple-browsers, [TB Parameters](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-shared/src/main/java/com/vaadin/testbench/Parameters.java)).
- Remote **Selenium Grid** and **Sauce Labs** are supported (Docs: e2e/test-grid, e2e/multiple-browsers).

## Extensibility

- **Custom or add-on components.** Write a class that `extends TestBenchElement` with `@Element("my-tag")` (optionally narrowed with `@Attribute(name=…, value=…/contains=…)`), then expose methods built from `$()`, `getProperty*`, `setProperty`, `callFunction`, `dispatchEvent`, and `executeScript`. Docs call such a class a page object and "in practice the same as" an element class (Docs: e2e/page-objects "Creating a Page Object", "Page Objects for Java Classes"; e2e/creating-tests TIP).
- **Extending existing wrappers.** Subclass an existing `*Element` to add or override behaviour (Docs: e2e/page-objects "Extending a Page Object").
- **Reusable traits.** The `Has*` mixin interfaces (`HasLabel`, `HasValidation`, …) are public and can be implemented by custom elements. `withCaption` and `withLabel` recognise them.
- **Discovery.** There is no registry or SPI: `$()` takes the class directly. Add-ons ship their own `*Element` jar by convention, as FC does with one `-testbench` artifact per component.

## Limits

TestBench Selenium **does** drive a real browser, so CSS, JS, layout, and screenshots all work. It has screenshot comparison with masking and highlighting, and screenshot-on-failure (Docs: e2e/screenshots). Its fundamental limits are different ones:

- **No access to server-side state.** Tests cannot touch Java `Component` instances, the `UI`, Spring beans, or the database in-process. Everything goes through the DOM, properties, and JS. Browserless has this access (Docs: `flow/testing/index.adoc` comparison).
- **Needs a running server plus a browser and driver.** It is slow (the docs contrast "600 UI tests in 7 seconds" for browserless with "1 to 2 hours" end-to-end) and subject to environment flakiness, which `RetryRule` and reruns exist for (Docs: `flow/testing/index.adoc`, e2e/reliable-tests).
- **Waiting covers Flow round-trips only.** There is no Playwright-style actionability auto-wait for element presence or visibility; see Waiting.
- **No ARIA role or accessible-name locators.**
- **No `data-testid` locator in a stable release yet** (25.4).
- **Values are mostly strings.** Many value setters, such as `TextFieldElement.setValue`, are **emulated** with synthetic DOM events rather than real typing. Use `sendKeys` for that.
- **Shadow DOM** is reachable only through TestBench queries or `getShadowRoot()`, not through plain `By` locators across shadow boundaries (Docs: e2e/getting-started NOTE).
- **Modern browser features** such as network interception, multiple contexts, and tracing are not provided by TestBench itself; only raw Selenium 4 is available underneath. Whether Selenium BiDi covers what the Unified API needs is **unverified**.
- **Escape Hatch exists by construction.** `getDriver()` returns the Selenium `WebDriver` (wrapped in the TestBench proxy), `executeScript(...)` is on the test case, and `getWrappedElement()` is on elements.

## License and maturity

- **License.** The TestBench core (`vaadin-testbench-shared`, `-core`, `-core-junit5`, `-core-junit6`) is under the **Vaadin Commercial License and Service Terms** (TB `LICENSE` and POM `<licenses>`). At class-load time `TestBench` calls `LicenseChecker.checkLicenseFromStaticBlock("vaadin-testbench", …, PRE_TRIAL)` ([TB TestBench.java](https://github.com/vaadin/testbench/blob/1ff7a491ed7f8c9877ecae9747634a4c28f5fb34/vaadin-testbench-shared/src/main/java/com/vaadin/testbench/TestBench.java)). "End-to-end testing requires a commercial TestBench subscription", whereas "browserless testing is free for all users" (Docs: `flow/testing/index.adoc`). CI needs a license key (Docs: e2e/ci-server).
- **Wrapper licensing.** The component wrapper jars (`vaadin-*-testbench`, `flow-html-components-testbench`) and the `vaadin-testbench-junit6` aggregator POM are **Apache-2.0** (Central POMs), but they are useless without the commercial core.
- **Ownership.** Vaadin Ltd. The source is public on GitHub (`vaadin/testbench`, `vaadin/flow-components`).
- **Maturity.** A long-lived product: Central versions of `vaadin-testbench-core` go back to 6.0.1, and JUnit 5 support dates from 9.0.0. It is actively developed (latest `main` commit 2026-10-30).
- **Versioning and supported Vaadin versions.** TestBench follows the platform via the `vaadin-bom` property `vaadin.testbench.version`:
  - Vaadin 24.9 → TestBench 9.5.0
  - Vaadin 25.0 → 10.0.0
  - Vaadin 25.1 → 10.1.0
  - Vaadin 25.2 → 25.2.0. Version numbers are aligned with the platform from 25.2 on (Central `vaadin-bom` POMs).
- **Baselines (current `main`).** Java 21 (`maven.compiler.release`), Selenium 4.52.0, Spring Boot 4.1.2 for its own tests (TB parent `pom.xml`).

## Unverified / open

- Whether `@SpringBootTest(RANDOM_PORT)` combines cleanly with `BrowserTestBase` / `@BrowserTest`. It is not documented on TestBench pages.
- Which Selenium 4 BiDi features (network interception, console capture beyond `getLogEntries`) work through the TestBench driver proxy.
- The release that will carry `withTestId`/`testId` (marked `@since 25.4`, still in alpha).
