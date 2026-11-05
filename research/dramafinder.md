# What does Dramafinder offer?

Research for issue #3 (map: #2). Question: what Dramafinder offers as a
**Testing Tool** that the **Unified API** would sit on top of.

Sources were read on 2026-11-05. Source code was read at
`parttio/dramafinder` `master` commit `a94bbb0` (version `1.2.5-SNAPSHOT`, after
release `1.2.4`). Links point to `master`.

Abbreviations used below:

- **[README]**: https://github.com/parttio/dramafinder/blob/master/README.md
- **[POM]**: https://github.com/parttio/dramafinder/blob/master/pom.xml
- **[TODO]**: https://github.com/parttio/dramafinder/blob/master/TODO.md
- **[MAPPING]**: https://github.com/parttio/dramafinder/blob/master/skills/vaadin-playwright-test/element-mapping.md
- **[API-REF]**: https://github.com/parttio/dramafinder/blob/master/skills/vaadin-playwright-test/api-reference.md (generated from source, also bundled in the jar at `META-INF/dramafinder/api-reference.md`)
- **[SRC]**: `src/main/java/org/vaadin/addons/dramafinder/`, https://github.com/parttio/dramafinder/tree/master/src/main/java/org/vaadin/addons/dramafinder
- **[CENTRAL]**: https://repo.maven.apache.org/maven2/org/vaadin/addons/dramafinder/maven-metadata.xml

## Summary

Dramafinder ("Drama Finder") is a library of typed **Playwright** wrappers
(`*Element` classes) for Vaadin web components. Each wrapper holds a Playwright
`Locator`. It has no dependency on a test base class or on the application:
anything that can supply a Playwright `Page` or `Locator` can use it. It is a
black-box, real-browser tool. It drives the DOM of the running app over HTTP and
cannot see server-side Java state. It covers about 50 Vaadin components, targets
Vaadin 25 and Java 21, and is compiled against JUnit Jupiter 6.0.1. JUnit is used
only by the optional base classes. It is Apache-2.0, a community add-on (not a
Vaadin product), and released to Maven Central as `org.vaadin.addons:dramafinder`.
The latest release is 1.2.4 (2026-11-03).

## Component API

**Covered components** ([TODO], "Components implemented"; one class per entry in [SRC]`element/`):
Accordion, App Layout, Avatar, Avatar Group, Badge, Big Decimal Field,
Breadcrumbs, Button, Card, Checkbox, Checkbox Group, Combo Box, Confirm Dialog,
Custom Field, Context Menu, Date Picker, Date Time Picker, Details, Dialog, Email
Field, Form Layout, Grid, Integer Field, List Box, Login Form, Login Overlay,
Markdown, Menu Bar, Message Input, Message List, Multi-Select Combo Box,
Notification, Number Field, Password Field, Popover, Progress Bar, Radio Button
(+ Group), Select, Side Navigation, Slider (Integer/Decimal), Split Layout,
Switch (experimental, Vaadin 25.3), Tab Sheet, Tabs, Text Area, Text Field, Time
Picker, Tooltip, Tree Grid, Upload, Virtual List. "Components not implemented"
is empty. The only shared interface listed as missing is `HasSize`.

**Shape of a wrapper** ([SRC]`element/VaadinElement.java`, `element/TextFieldElement.java`):

- Abstract base `VaadinElement(Locator)`. It provides `click()`, `getText()`,
  `getLocator()`, `isVisible()`, `assertVisible()`, `assertHidden()`, and raw
  `getProperty(name)` / `setProperty(name, value)`, which evaluate JS on the
  element.
- Each class carries `@PlaywrightElement("vaadin-text-field")` (the web
  component tag) and a `FIELD_TAG_NAME` constant.
- Behaviour is composed from about 20 mixin interfaces in `element/shared/`:
  `HasValueElement`, `HasLabelElement`, `HasHelperElement`,
  `HasValidationPropertiesElement`, `HasEnabledElement`, `HasReadOnlyElement`,
  `HasPlaceholderElement`, `HasPrefixElement`/`HasSuffixElement`,
  `HasClearButtonElement`, `HasThemeElement`, `HasStyleElement`,
  `HasTooltipElement`, `HasAriaLabelElement`, `FocusableElement`,
  `HasCheckedElement`, and others ([TODO], "Shared interfaces implemented").
- Methods come in three families:
  - actions: `setValue(..)`, `selectItem(..)`, `click()`, `open()`
  - getters: `getValue()`, `getMinLength()`, `isOpened()`
  - `assertX(..)` methods: `assertValue(..)`, `assertInvalid()`,
    `assertErrorMessage(..)`, `assertRowCount(..)`. These wrap Playwright's
    retrying `assertThat(locator)` assertions.
- Values are mostly strings at the DOM level. Typed overloads exist where the
  component needs them: `DatePickerElement.setValue(LocalDate)`,
  `TimePickerElement.setValue(LocalTime)`,
  `DateTimePickerElement.setValue(LocalDateTime)`, and `SliderElement`
  `setValue(double)` ([MAPPING]).
- Every wrapper exposes inner locators such as `getInputLocator()`,
  `getHelperLocator()`, and `getContentLocator()` for plain Playwright use
  ([README] "Usage").
- Some wrappers have "setters" for client-side properties, for example
  `TextFieldElement.setMinLength(int)`. These change the web component in the
  browser only and bypass the server ([SRC]`element/TextFieldElement.java`).
- `GridElement` is large ([SRC]`element/GridElement.java`). It has row and
  cell access by index or header text (`findCell(row, col)` returns an
  `Optional<CellElement>`), header and footer cells, sorting
  (`HeaderCellElement.clickSort()`), selection (`select(row)`,
  `checkSelectAll()`), scrolling (`scrollToRow`), details rows,
  `waitForGridToStopLoading()`, and about 30 `assert*` methods.
  `TreeGridElement` extends it with expand and collapse.

## Locating

Locating uses static factories on each element class, plus plain constructors
that take a `Locator` ([MAPPING] "Factory method conventions"). Factories are
**not uniform**. Each class has its own set:

| Strategy | How | Where |
|---|---|---|
| Accessible name (label, `aria-label`, placeholder fallback) | `XElement.getByLabel(page \| locator, label)` | All fields, plus Button, MenuBar, SideNav, Popover, Breadcrumbs, and others |
| Visible text | `getByText` (Button, Notification, Badge, Tooltip), `getByHeaderText` (Dialog, ConfirmDialog), `getBySummaryText` (Details), `getByTitle` (Card, LoginOverlay), `getByButtonText` (Upload), `getByName` (Avatar) | per class |
| First of a tag on the page or in a scope | `XElement.get(page)` / `get(locator)` | Containers: Grid, VirtualList, TabSheet, AppLayout, FormLayout, and others |
| DOM id | `getById(page, id)` | Only AppLayout, FormLayout, Grid, TreeGrid, Tabs |
| Anything else (CSS, ARIA role, test id, chains) | `new XElement(page.locator(...))` | Every class has a public `Locator` constructor |
| Typed child by wrapper class | `CustomFieldElement.getField(TextFieldElement.class, index)` reads the class's `@PlaywrightElement` tag reflectively | CustomField, MultiSelectComboBox, VirtualList only |

How the accessible-name lookup works ([SRC]`element/utils/AccessibleNameLocator.java`):
`page.locator(tag).filter(has: getByRole(role, name).or(getByPlaceholder(label))).first()`.
By default the match is **case-insensitive substring, first match wins**, so
"Name" also matches "Nickname". `findExact` is the strict variant: it matches the
exact name, has no placeholder fallback, and fails in strict mode on ambiguity.
Scoping comes from the `(Locator, label)` overloads.

There is **no generic query builder** like TestBench's `$(TextFieldElement.class).id(..)`
and no lookup by Java component class or by Flow server-side id. The
`@PlaywrightElement` annotation exists, but it is used only by the three
typed-child helpers above (verified by grep of [SRC]).

## Waiting

Waiting relies mainly on Playwright, with a few Vaadin-specific waits:

1. **Playwright auto-wait.** Actions (`click`, `fill`) wait until the element is
   actionable. `assert*` methods use `PlaywrightAssertions.assertThat`, which
   retries until the timeout. Getters (`getValue()`, `getMinLength()`) do
   **not** wait. The README warns to use `assertX` rather than
   `assertEquals(x, getX())` ([README] "Note").
2. **Polling assertions for computed state.** The package-private
   `AssertionWaits.assertEventually(...)` polls with `Page.waitForCondition` and
   turns timeouts into `AssertionError`s that state the expected and actual
   values ([SRC]`element/AssertionWaits.java`).
3. **Flow round-trip sync is explicit, not automatic.**
   `HasLocatorElement.waitForVaadinIdle()` evaluates `WAIT_FOR_VAADIN_SCRIPT`.
   The script returns true when no `window.Vaadin.Flow.clients[*].isActive()`
   ([SRC]`element/shared/HasLocatorElement.java`,
   `AbstractBasePlaywrightIT.java`). It is called:
   - after navigation in the base class
   - in `HasValueElement.setValue` (fill, dispatch `change`, wait idle)
   - in a few elements: DatePicker, DateTimePicker, Slider, CheckboxGroup
   - before opening `ContextMenuElement.openOn`, which also retries the
     right-click when an in-flight round trip replaced the menu

   `click()` does **not** wait for the server (11 call sites in total).
   `GridElement.waitForGridToStopLoading()` waits for the grid's `loading`
   attribute to clear.
4. **Timeouts.** The base class sets `page.setDefaultTimeout(15000)` and
   `setDefaultNavigationTimeout(4000)`. Otherwise the Playwright defaults apply.

## JUnit support

- The element API (`element/**`) imports no JUnit. It works under any runner
  (grep of [SRC]).
- JUnit appears only in the optional base classes. `AbstractBasePlaywrightIT`
  and `agent/VisualVerificationTest` use `@BeforeAll`, `@BeforeEach`,
  `@AfterEach`, `@AfterAll`, and `@ExtendWith`. The extension
  `agent/AgentReporting` implements `AfterTestExecutionCallback`. All of these
  are **JUnit Jupiter** only. There is no JUnit 4 or TestNG support.
- [POM] declares `org.junit.jupiter:junit-jupiter-api:6.0.1` with
  `provided` scope. That is **JUnit 6**. Release poms 1.0.2, 1.1.6, and 1.2.4
  all declare 6.0.1 (read from tags).
- The library's own ITs run on Spring Boot 4.x (`spring-boot-dependencies`
  4.0.0 in 1.0.2, 4.1.1 now). *Unverified:* that Spring Boot 4 manages JUnit
  Jupiter 6.x for those test runs. The direct `provided` 6.0.1 dependency is
  verified.
- Browser and Playwright state live in `ThreadLocal`s with one browser per test
  class, so parallel JUnit execution is anticipated
  ([SRC]`AbstractBasePlaywrightIT.java`). Whether it is tested is unverified.

## App lifecycle

Dramafinder **does not start the application.** It needs a URL that is already
being served ([README] "Getting started"):

- **Spring Boot.** Users add `@SpringBootTest(webEnvironment = RANDOM_PORT)`
  plus `@LocalServerPort` and return `http://localhost:<port>/` from `getUrl()`.
  The bundled Claude skill generates a two-line `SpringPlaywrightIT` subclass
  for this
  ([templates/SpringPlaywrightIT.java.tmpl](https://github.com/parttio/dramafinder/blob/master/skills/vaadin-playwright-test/templates/SpringPlaywrightIT.java.tmpl)).
  The library itself does not ship this class and does not depend on Spring.
- **External server.** Run ITs with Failsafe and start or stop the server around
  them, for example with `spring-boot:start` / `spring-boot:stop` in
  `pre-`/`post-integration-test`, then navigate to that URL ([README]).
- **Base class contract.** `HasTestView` has `getUrl()` and `getView()`.
  `AbstractBasePlaywrightIT` opens a new page per test, navigates to
  `getUrl() + getView()`, and waits for Vaadin idle. The browser is
  **Chromium only**, headless unless `-Dheadless=false` or `HEADLESS=false`.
  For Firefox or WebKit, users write their own setup with plain Playwright.
- `VisualVerificationTest` targets an already-running app at
  `dramafinder.agent.baseUrl` / `DRAMAFINDER_BASE_URL` (default
  `http://localhost:8080`).
- No built-in support for plain servlet containers or Quarkus. Nothing prevents
  them, because only a URL is needed.

## Extensibility

- **Custom or add-on components:** subclass `VaadinElement`, take a `Locator` in
  the constructor, annotate with `@PlaywrightElement("my-tag")`, and implement
  the shared mixins that fit (`HasValueElement`, `HasLabelElement`, ...) to
  inherit their default methods. Add static factories that follow the
  `getByLabel(Page, String)` convention ([MAPPING]; AGENTS.md "Best Practices":
  https://github.com/parttio/dramafinder/blob/master/AGENTS.md). Subclass an
  existing element to specialise it (`EmailFieldElement extends
  TextFieldElement`).
- Mixins are interfaces with default methods over `getLocator()`, so add-on
  wrappers reuse them freely.
- **Fallback:** raw Playwright through `getLocator()` and inner locators, or
  `getProperty`/`setProperty` for JS properties. These are the natural **Escape
  Hatch** points.
- There is no registry or discovery mechanism. Wrappers are plain classes.

## Limits

These are fundamental to the design (black box, real browser):

- **No server-side access.** It cannot read or set Java component state, call
  server methods, or inspect `UI`/`VaadinSession`. Assertions see only the DOM
  and web-component properties.
- **No lookup by Java type or Flow id** (see Locating). Tests are written
  against tags, labels, and text, not `TextField.class`.
- **Setters are client-side.** `setProperty`/`setMinLength`-style methods mutate
  the browser element. The server is not told unless an event propagates.
- **Needs a running server, a real browser, and Playwright's browser binaries.**
  It is slower than an in-JVM tool, but it renders CSS and JS and supports
  screenshots: `page.screenshot` directly; the `agent` package writes failure
  screenshots and component snapshots to `target/agent-report/`.
- **Coverage gaps:** `HasSize` is not implemented, and factory sets are uneven
  across components ([TODO], [MAPPING]).
- **Only one Vaadin line** is supported (25; see below).
- Fuzzy `getByLabel` (substring, first match) can silently pick the wrong
  field. Use `findExact` for strictness.

## License and maturity

- **License:** Apache 2.0 ([POM] `<licenses>`; `LICENSE` file; GitHub repo
  metadata `Apache-2.0`).
- **Ownership:** GitHub `parttio/dramafinder`. The POM gives organization
  "Vaadin Community" and developer "JC Gueriaud". It is a community add-on, not
  a supported Vaadin product. The ticket assumed it lives under
  `github.com/vaadin`, but no repository was found there. *Unverified:* whether
  Vaadin Ltd. formally supports or endorses it. The Vaadin Directory page
  (vaadin.com) could not be reached from this environment.
- **Coordinates:** `org.vaadin.addons:dramafinder` ([POM]). The README snippet
  still shows `1.2.0`.
- **Releases on Maven Central** ([CENTRAL]): 0.0.4, 1.0.2, 1.1.0–1.1.6 (no
  1.1.3), 1.2.0, 1.2.3, 1.2.4 (latest and release, `lastUpdated` 2026-11-03).
  Git tags date 0.0.1 to 2025-09-25, 1.0.x to 2025-12-12, 1.1.0 to 2026-03-03,
  1.2.0 to 2026-09-24, and 1.2.4 to 2026-11-03. Development is active, with
  several releases a month recently. The project follows semantic versioning
  since 1.0 ([README] "Note").
- **Supported Vaadin versions:** "built and tested against Vaadin 25"
  ([README]). The build uses `vaadin.version`: 25.0.0-rc1 at 1.0.2, 25.1.3 at
  1.1.6, and 25.3.0 now. **Vaadin 24 is not claimed.** The library has no
  compile dependency on Vaadin, so it might work against 24 DOM, but that is
  untested and unverified.
- **Runtime requirements:** Java 21 (`maven.compiler.release` 21; CI uses JDK 21).
  Playwright Java `com.microsoft.playwright:playwright` is a compile dependency
  (1.63.0 now, 1.55.0 at 1.0.2).

## Notes for downstream decisions

- **Locating:** Dramafinder's main idiom is accessible name or text, not id or
  component class. A Unified API that locates by `id` or by component class has
  to translate those to `#id` or tag CSS on this Testing Tool. Locating by label
  is native here, but needs care on Browserless and TestBench.
  Exact-versus-substring label semantics must be decided explicitly.
- **Waiting:** Dramafinder waits mostly by retrying assertions. Server
  round-trip sync is explicit (`waitForVaadinIdle`) and only built into
  `setValue` and a few components; `click()` does not sync. The Unified API's
  waiting model must decide whether actions auto-sync, as TestBench does.
  Getter-then-`assertEquals` is flaky on this tool.
- **Testing Tool selection:** the element API needs only a `Page`, so an adapter
  can own Playwright and browser setup itself and does not have to extend
  `AbstractBasePlaywrightIT`. The base class is Chromium-only, has hard-coded
  timeouts, and uses one browser per class.
- **Escape Hatch:** `getLocator()` (Playwright `Locator`), `getLocator().page()`,
  and `getProperty`/`setProperty` are the natural escape hatches.
- **Unsupported features:** no server-side state access, Vaadin 25 only, Java
  21+, Jupiter only. The Upload API takes `Path` files. Typed values exist only
  for date, time, and slider fields; other values are strings.
