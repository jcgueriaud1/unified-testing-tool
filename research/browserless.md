# What does Browserless testing offer?

Research for issue #4 (map: #2). Question: what Browserless testing (formerly
Vaadin TestBench "UI unit testing") offers as a **Testing Tool** that the
**Unified API** would sit on top of.

Sources were read on 2026-11-05:

- `vaadin/browserless-test` `main` at commit `cc6e623` (version `25.4-SNAPSHOT`,
  after release `25.3.2`).
- `vaadin/testbench` `main` at commit `1ff7a49` (version `25.4-SNAPSHOT`), for
  the deprecated predecessor modules.
- `vaadin/docs` `main` at commit `2b8bbb8`, the source of vaadin.com/docs.
  (vaadin.com itself was not reachable from the research environment, so the
  AsciiDoc sources were read instead. They are the same text the site renders.)
- Maven Central metadata (`repo1.maven.org`).

`GLOSSARY.md` does not exist yet at the branch point, so this file uses the
terms as the map (#2) defines them: **Unified API**, **Testing Tool**,
**Escape Hatch**.

Abbreviations used below:

- **[README]**: https://github.com/vaadin/browserless-test/blob/main/README.md
- **[BL-SRC]**: https://github.com/vaadin/browserless-test/tree/main/shared/src/main (core: `com.vaadin.browserless`, testers in `com.vaadin.flow.component.*`)
- **[BL-JUNIT6]**: https://github.com/vaadin/browserless-test/tree/main/junit6/src/main/java/com/vaadin/browserless
- **[BL-POM]**: https://github.com/vaadin/browserless-test/blob/main/pom.xml
- **[FLOW-VERSION]**: https://github.com/vaadin/browserless-test/blob/main/guidelines/flow-version.md
- **[TESTERS-GUIDE]**: https://github.com/vaadin/browserless-test/blob/main/guidelines/testers.md
- **[DOCS]**: https://github.com/vaadin/docs/tree/main/articles/flow/testing/browserless (rendered at https://vaadin.com/docs/latest/flow/testing/browserless)
- **[HOWTO]**: https://github.com/vaadin/docs/tree/main/articles/building-apps/testing/browserless (rendered at https://vaadin.com/docs/latest/building-apps/testing/browserless)
- **[TESTING-INDEX]**: https://github.com/vaadin/docs/blob/main/articles/flow/testing/index.adoc
- **[TB]**: https://github.com/vaadin/testbench (modules `vaadin-testbench-unit*`)
- **[CENTRAL]**: https://repo1.maven.org/maven2/com/vaadin/

## Summary

Browserless testing runs Vaadin Flow UI tests **in the test JVM, with no
browser and no servlet container**. It mocks the Vaadin service, session and
UI, and the test calls server-side Java components directly. It is a
white-box, server-side tool. It finds components by **Java type** in the
server-side component tree, and drives them through typed **testers**
(`ComponentTester` subclasses) that simulate user input. No client-side
JavaScript runs.

The product has moved. TestBench "UI Unit Testing"
(`com.vaadin.testbench.unit`, commercial, JUnit 4 and 5) is deprecated for
removal. Its successor is **Vaadin Browserless Test**
(`com.vaadin:browserless-test-*`, package `com.vaadin.browserless`). It is
Apache-2.0, free since Vaadin 25.1, built for **JUnit 6** only, and versioned
with Vaadin (25.3.x today). The Unified API should target the new artifact.
Its lineage goes back to Karibu-Testing.

## The product and its predecessor

| | TestBench UI Unit Testing (old) | Browserless Test (new) |
|---|---|---|
| Artifacts | `vaadin-testbench-unit` (JUnit 4), `vaadin-testbench-unit-junit5`, `vaadin-testbench-unit-quarkus` | `browserless-test-junit6`, `-shared`, `-spring`, `-quarkus`, `-bom` |
| Package | `com.vaadin.testbench.unit` | `com.vaadin.browserless` (testers stay in `com.vaadin.flow.component.*`) |
| Base classes | `UIUnit4Test`, `UIUnitTest`, `SpringUIUnitTest`, `QuarkusUIUnitTest` | `BrowserlessTest`, `SpringBrowserlessTest`, `QuarkusBrowserlessTest` |
| Status | `@Deprecated(forRemoval = true, since = "10.1")` on every public class | Active |
| License | Vaadin Commercial License and Service Terms | Apache-2.0 |

Sources: deprecation javadoc ("Replace the vaadin-testbench-unit dependency
with browserless-test-junit6 and use the corresponding class from the
com.vaadin.browserless package instead") on, for example,
`vaadin-testbench-unit-junit5/.../UIUnitTest.java` [TB]. The license is in
[TB] `LICENSE` and the module POMs. The rename table and the migration steps
are in [HOWTO] `migrate-ui-unit-tests.adoc`. The modules table is in [README].

The docs say: "Browserless testing was previously known as _UI Unit Testing_
and required a commercial TestBench subscription. Browserless testing is free
for all users" since Vaadin 25.1 ([DOCS] `index.adoc`). The old page
`flow/testing/ui-unit` now only says "This page has moved" to Browserless
Testing.

Migration from the JUnit 5 base classes is mostly a rename. `$()` and
`$view()` become `find()` and `findInView()`; the old names are deprecated
aliases. JUnit 4 tests can't share a classpath with `browserless-test-junit6`,
because both artifacts ship testers with the same fully qualified names, such
as `com.vaadin.flow.component.button.ButtonTester` ([HOWTO]
`migrate-ui-unit-tests.adoc`).

Ancestry: the Kotlin internals (`MockVaadin`, `TestingLifecycleHook`,
`Grid.kt`) keep comments that point to `mvysny/karibu-testing` issues ([BL-SRC]
`kotlin/com/vaadin/browserless/internal/TestingLifecycleHook.kt`,
`component/Grid.kt`). Karibu DSL and DynaTest remain test-only dependencies
([BL-JUNIT6] `pom.xml`). That the code base was derived from Karibu-Testing is
inferred from these references. No primary statement of the relationship was
read.

## Component API

**Shape.** A tester wraps one component instance:
`test(component)` returns the tester that matches the component's type, and
`test(TextFieldTester.class, field)` asks for one explicitly ([DOCS]
`component-testers.adoc`). Testers do actions (`click()`, `setValue(..)`,
`selectItem(..)`) and some inspection (`GridTester.getCellText`,
`NotificationTester.getText`). Reading state otherwise goes through the
component's own Java API: "Reading state is done via the component API
directly" (`textField.getValue()`).

Every changing action first calls `ensureComponentIsUsable()`. It throws
`IllegalStateException` unless the component is visible (the whole parent
chain), enabled, attached, not behind a modal or inert, and, for a value
component, not read-only ([DOCS] `component-testers.adoc` "Usability Checks";
[TESTERS-GUIDE]). Interactions go through the client path, so value and DOM
events report `isFromClient() == true` ([TESTERS-GUIDE]). Testers apply browser
constraints: a too-long value throws, and `min`/`max` only mark the field
invalid ([DOCS] "Constraint Enforcement").

**Base methods** (`ComponentTester` plus the `Clickable` mixin, [BL-SRC]):
`getComponent()`, `isUsable()`, `ensureComponentIsUsable()`, `click()`,
`click(MetaKeys)`, `middleClick()`, `rightClick()`, `focus()`, `blur()`,
`isFocused()`, `find(Class)` (a query scoped to the component), and the
protected helpers `roundTrip()`, `fireDomEvent(..)`, `setValueAsUser(..)`.

**Coverage.** 84 `*Tester` classes ship in `browserless-test-shared` ([BL-SRC],
counted from source). The README says "70+ built-in component testers".

- Fields: `TextFieldTester` (also covers `PasswordField`, `EmailField`,
  `BigDecimalField`), `TextAreaTester`, `NumberFieldTester`,
  `DatePickerTester`, `TimePickerTester`, `DateTimePickerTester`,
  `CheckboxTester`, `CheckboxGroupTester`, `RadioButtonTester`,
  `RadioButtonGroupTester`, `SelectTester`, `ComboBoxTester`,
  `MultiSelectComboBoxTester`, `ListBoxTester`, `MultiSelectListBoxTester`,
  `SwitchTester`, the slider testers (`Integer|Decimal|Number` and their
  `Range` variants).
- Actions and navigation: `ButtonTester`, `MenuBarTester`,
  `ContextMenuTester`, `GridContextMenuTester`, `RouterLinkTester`,
  `SideNavTester`, `BreadcrumbsTester`, `TabsTester`, `TabSheetTester`,
  `AnchorTester`.
- Data: `GridTester`, `TreeGridTester`, `GridProTester`,
  `VirtualListTester`.
- Overlays: `DialogTester`, `ConfirmDialogTester`, `NotificationTester`,
  `PopoverTester`, `LoginOverlayTester`, `LoginFormTester`.
- Layout and containers: `AccordionTester`, `DetailsTester`,
  `SplitLayoutTester`, `MasterDetailLayoutTester`, `CardTester`,
  `DashboardTester`, `AvatarGroupTester`.
- Others: `UploadTester` (and `UploadButton`/`DropZone`/`FileList`),
  `MessageInputTester`, `MessageListTester`, `MarkdownTester`, `ChartTester`
  (commercial component).
- HTML: `Div`, `Span`, `Paragraph`, `H1`–`H6`, `Image`, `Input`, `NativeButton`,
  `NativeLabel`, `NativeDetails`, lists, `Pre`, `Hr`, `Emphasis`, `RangeInput`,
  `Text`.

**Typical method signatures** (from [DOCS] `component-testers.adoc` "Common
Testers" and [BL-SRC]):

- `ButtonTester`: `click()`.
- `TextFieldTester`: `setValue(V)`, `clear()`, `clickClearButton()`.
- `CheckboxTester`: `click()`, `check()`, `uncheck()`.
- `SelectTester` and `ComboBoxTester`: `selectItem(String label)`. This takes
  the **display label**, not the typed item.
- `GridTester<T, Y>`: `size()`, `getRow(int)`, `clickRow(int[, MouseButton][, MetaKeys])`,
  `doubleClickRow`, `select(int)`, `deselect(int)`, `selectAll()`,
  `deselectAll()`, `getSelected()`, `getCellText(row, col)`,
  `getCellComponent(row, col|key)`, `getHeaderCell(int)`,
  `sortByColumn(..)`, `getLitRendererPropertyValue(..)`, `contextMenu(row)`.
- `DialogTester`: `open()`, `close()`, `pressEscape()`, `clickOutside()`,
  `findInHeader(Class)`, `findInFooter(Class)`.
- `UploadTester`: `upload(File)`, `uploadAll(File...)`,
  `getLastUploadStatus()`, `ensureUploaded()`.

**Locators (since 25.2).** These are a typed, fluent layer that joins a query
and a tester: `findButton().withText("Save").click()`,
`findTextField().withLabel("Name").setValue("Ada")`. Value types that the
component does not fix take a witness: `findGrid(Person.class).getRow(0)`. A
locator resolves on its first action and caches the result. `invalidate()`
clears it. `component()`, `components()` and `exists()` are also available
([DOCS] `locators.adoc`; [README] "Locators API").

Each locator only offers the filters its component supports, through mixins
(`HasTextFilter`, `HasLabelFilter`, `HasAriaLabelFilter`,
`HasPlaceholderFilter`, `HasValueFilter`, `HasThemeFilter`), so
`findButton().withLabel(..)` does not compile. An annotation processor
generates the locators from the `@Tests`-annotated testers (`LocatorProcessor`,
"Internal build tool. This artifact is not published").

Windows (`BrowserlessUIContext`) and the JUnit extensions expose locators by
default. Base-class tests opt in with `implements Locators`, or
`CommercialLocators` for Charts, GridPro and other commercial components.

## Locating

The search walks the **server-side component tree**. It never queries a DOM.

- **Entry points:** `find(Class<T>)` (from the UI root), `findInView(Class<T>)`,
  `find(Class<T>, Component root)`, a tester's `find(Class)`, and chaining with
  `thenOn(index, Class)` and `thenOnFirst(Class)`. The **component class is
  always required** ([DOCS] `component-query.adoc`).
- **Filters on `ComponentQuery`:** `withId`, `withTestId` (`data-testid` set by
  `Component.setTestId()`), `withText`, `withTextContaining`, `withCaption`,
  `withLabel`, `withLabelContaining`, `withAriaLabel`,
  `withAriaLabelContaining`, `withPlaceholder` (25.2), `withClassName`,
  `withoutClassName`, `withTheme(ThemeVariant)`, `withAttribute(name[, value])`,
  `withoutAttribute`, `withinSlot(name)` (25.3), `withValue(V)`,
  `withPropertyValue(getter, V)`, `withCondition(Predicate)`, and the
  result-count assertions `withResultsSize`, `withMinResults` and
  `withMaxResults` ([DOCS] `component-query.adoc` "Filtering Methods").
- **Terminal operations:** `single()`, `first()`, `last()`, `atIndex(int)`
  (1-based), `all()`, `exists()`, `id(String)`, `testId(String)`.
- **What it does not offer:** CSS selectors, XPath, and ARIA **role** queries.
  No such API was found in [BL-SRC]. ARIA support is limited to the
  `aria-label` attribute.
- **What it cannot see:** components that a renderer creates per item (for
  example a Grid component column) and the content of closed overlays. The
  query then returns an empty result rather than an error. These are reached
  through the owning tester: `GridTester.getCellComponent(row, col)`, or by
  opening the overlay first ([DOCS] `component-query.adoc` "Components a Query
  Cannot Find"; [README]). Child components of Lit or Polymer templates are
  not listed ([BL-SRC] `TestingLifecycleHook.kt`, the `isTemplate` branch).

## Waiting

There is no auto-wait, no explicit wait API and no timeout model. Execution is
**synchronous** on the test thread:

- Each tester action runs the server-side listeners before it returns.
- Before every component lookup, `TestingLifecycleHook.awaitBeforeLookup()`
  calls `MockVaadin.clientRoundtrip()`. It runs the queued `UI.access()`
  tasks and the `beforeClientResponse` callbacks, and cleans up closed dialogs
  ([BL-SRC] `TestingLifecycleHook.kt`, `MockVaadin.kt`).
- `roundTrip()` triggers the same flush on demand. "It does not start a
  browser or execute client-side JavaScript" ([DOCS] `test-environment.adoc`).
- Signals are the one exception. Signal effects and shared-signal
  confirmations are queued and run by `runPendingSignalsTasks()`, which waits
  up to 100 ms by default for the first task. An overload takes a maximum
  wait ([README] "Signals"; [DOCS] `testing-signals.adoc`).
- Exceptions thrown on the server propagate into the test method ([TESTING-INDEX]
  "Robust").

The docs describe this as a design advantage: "No arbitrary sleeps are needed,
since the test runs on the server side and can wait until the request is
fully processed" ([TESTING-INDEX]). The docs do not describe work on
background threads that does not go through `UI.access()` or signals.
Synchronising with such work is not documented.

## JUnit support

- **Browserless Test: JUnit 6 (Jupiter) only.** The single test-framework
  module is `browserless-test-junit6` [README]. Spring and Quarkus are built on
  top of it. It provides the `BrowserlessTest` base class
  (`@BeforeEach initVaadinEnvironment()` and `@AfterEach`), the
  `BrowserlessExtension` (per method) and `BrowserlessClassExtension` (per
  class, shared environment) extensions, `BrowserlessTestConfigExtension`, and
  `TreeOnFailureExtension` ([BL-JUNIT6]; [DOCS] `extensions.adoc`, "JUnit 6
  Extensions").
- The JUnit version is not pinned in the module's POM. It comes from
  `spring-boot-dependencies` `4.1.1` ([BL-POM]). Saying that this resolves to
  JUnit 6.x is an inference from Spring Boot 4's dependency management, not
  verified here.
- The **application-context API** (`BrowserlessApplicationContext`,
  `BrowserlessUserContext`, `BrowserlessUIContext`) is plain `AutoCloseable`
  Java in `-shared`. Tests drive it from an ordinary `@Test` method with
  try-with-resources, so it does not depend on the JUnit lifecycle ([README]
  "Multi-user and multi-window testing"; [DOCS] `test-environment.adoc`, which
  calls it "the main entry point").
- No JUnit 4 or TestNG support. JUnit 4 users must stay on the deprecated
  `vaadin-testbench-unit` in a separate module ([HOWTO]
  `migrate-ui-unit-tests.adoc`).
- Old line: `vaadin-testbench-unit-junit5` is released up to 25.3.2 on
  [CENTRAL]. A `vaadin-testbench-unit-junit6` module exists on TestBench `main`
  (JUnit `6.0.0`, reusing the JUnit 5 sources) [TB]. Whether it has been
  published to Maven Central is **unverified** (rate-limited when checked). It
  is deprecated in any case.

## App lifecycle

There is no server, no port and no URL. The test **creates** the application
in-process. It cannot connect to an external or already-running app.

- **Plain Java:** `BrowserlessTest`, the JUnit extensions, or
  `BrowserlessApplicationContext.create(viewPackagesOrClasses)`. These build a
  mock `VaadinServletService`, `VaadinSession` and `UI` per test (or per class
  with `BrowserlessClassExtension`) and open the root route. Routes come from a
  classpath scan, restricted with `@ViewPackages(classes=..., packages=...)`
  ([DOCS] `test-environment.adoc`).
- **Spring Boot:** `browserless-test-spring` provides `SpringBrowserlessTest`,
  used with `@SpringBootTest` or `@ContextConfiguration`, and
  `SpringBrowserlessApplicationContext.create(springCtx, ...)`. Beans come from
  the Spring context. `@WithMockUser` and similar annotations apply before the
  first navigation. `@VaadinSessionScope` beans can't be injected into the
  test instance, because no session exists yet ([DOCS]
  `environment-differences.adoc`, `spring-security.adoc`).
- **Quarkus:** `browserless-test-quarkus` provides `QuarkusBrowserlessTest`
  with `@QuarkusTest` and `QuarkusBrowserlessApplicationContext`.
  `@TestSecurity` is supported ([DOCS] `quarkus.adoc`).
- **Jakarta EE/CDI:** the application writes its own `AbstractCdiViewTest`,
  using Weld (`weld-junit5`) and `CdiVaadinServlet`. Locators and the
  application-context API don't apply this setup ([DOCS] `cdi.adoc`,
  `extensions.adoc`).
- **Standalone component:** `BrowserlessUIContext.forComponent(component)`
  needs no route (25.2) ([DOCS] `component-testers.adoc`).
- **Extras:** several users and windows in one test, with the security
  context switched per user. Per-test configuration with
  `@BrowserlessTestConfig` (application properties, feature flags, `Lookup`
  services). `reload()` simulates F5 and honours `@PreserveOnRefresh`.
  `getExternalNavigationURL()` captures `Page.setLocation` and `Page.open`
  ([README]; [DOCS] `multi-user.adoc`, `test-configuration.adoc`).

## Extensibility

- **Custom tester:** extend `ComponentTester<C>` and annotate it with
  `@Tests(MyComponent.class)`, or `@Tests(fqn = "...")`. Action methods call
  `ensureComponentIsUsable()` and use the protected helpers
  (`setValueAsUser`, `setPropertyAsUser`, `fireDomEvent`) so that events stay
  `isFromClient()`. The tester is discovered by scanning
  `com.vaadin.flow.component` by default, plus packages added with
  `@ComponentTesterPackages` or `withComponentTesterPackages(..)` ([DOCS]
  `component-testers.adoc` "Custom Tester Contracts"; [TESTERS-GUIDE]).
- **Custom locator:** extend `Locator<C, SELF>`, add filter mixins such as
  `HasLabelFilter`, and compose the built-in locators with `inside(this)`.
  Tests call it with `find(MyLocator::new)`. The docs call this "the lightest
  way to wrap the interactions with your own components" ([HOWTO]
  `test-custom-components.adoc`). Users don't get generated `findXxx()`
  methods for their own testers, because the annotation processor is internal
  and not published.
- **Without a wrapper:** any component can be found by class and used through
  its Java API. A component without a tester can be used through a locator
  that subclasses `ComponentTester` privately to reach `setValueAsUser` (worked
  example: `LocationFieldLocator` in [HOWTO] `test-custom-components.adoc`).

## Limits

- **No browser:** no client-side JavaScript, no web-component behaviour, no
  CSS, no rendering or layout, and no cross-browser testing ([TESTING-INDEX]
  "Advantages of End-to-End Testing": "end-to-end testing enables you to test
  the client-side functionality ... These kinds of tests are impossible with
  browserless testing"; [DOCS] `test-environment.adoc`).
- **No screenshots:** the "snapshot" is a text dump of the server-side
  component tree. "It represents Java component state and element attributes,
  not a browser screenshot or rendered layout" ([DOCS] `snapshots.adoc`).
- **Visibility is server-side only:** "visible" means the
  `setVisible`/parent-chain state. CSS-hidden or off-screen elements count as
  visible ([TESTERS-GUIDE] "Usability").
- **Only the Java tree is visible:** see "Locating" (renderers, closed
  overlays, template children). `LitRenderer` columns have no server
  component.
- **Pending JavaScript is mostly dropped:** focus simulation reads Flow's
  pending-JavaScript queue and drops other JavaScript queued at the same time
  ([README] "Focus and blur", issue `vaadin/browserless-test#221`). Some
  testers fire events directly, without focus side effects, for example radio
  button, menu bar and context-menu clicks.
- **Same JVM only:** no external URL, and no non-Flow (Hilla/React) views.
  Flow views only, as the base classes scan for `@Route` views.
- **One version, one Vaadin:** each Browserless Test release targets exactly
  one Vaadin/Flow minor version ([FLOW-VERSION]).

## License and maturity

- **License:** Apache-2.0 ([README], [BL-POM] `<licenses>`, file headers). The
  predecessor `vaadin-testbench-unit*` is under the Vaadin Commercial License
  ([TB]). Browserless testing is "free for all users" since Vaadin 25.1
  ([DOCS] `index.adoc`). End-to-end TestBench still needs a commercial
  subscription ([TESTING-INDEX]).
- **Owner:** Vaadin Ltd, in the GitHub org `vaadin/browserless-test`.
  `groupId` is `com.vaadin`. Its versions are managed by the Vaadin BOM
  ([DOCS] `getting-started.adoc`: "The BOM defines the versions of the Vaadin
  artifacts").
- **Release status:** `browserless-test-shared` and `-quarkus` on [CENTRAL] go
  from `1.0.0-beta1` through `1.0.x`, `1.1.x`, `1.2.0-rc1` to `25.3.0`–`25.3.2`
  (latest `25.3.2`, updated 2026-10-14). The git tags show `25.4.0-alpha1`
  to `alpha3` in progress. The `browserless-test-junit6` metadata could not be
  fetched (HTTP 429). Its version is assumed to follow the same train
  (**unverified**).
- **Version mapping** ([FLOW-VERSION]): `1.0.x` targets Vaadin 25.1,
  `1.1.x` targets 25.2, `25.3.x` targets 25.3, and `main`
  (`25.4-SNAPSHOT`) targets 25.4. From 25.3 the version number is the Vaadin
  version. Requires Java 21+ and Vaadin 25.1+ [README]. Vaadin is a `provided`
  dependency.
- **API stability:** some APIs are still moving. `$()` and `$view()` are
  deprecated "for removal in 26.0". Locators "may become the default in a
  future release". Spring support moved to its own artifact in 25.2 ([DOCS]
  `component-query.adoc`, `locators.adoc`; [HOWTO]
  `migrate-ui-unit-tests.adoc`).

## Notes for downstream decisions

- **Testing Tool selection:** "Browserless" should mean `browserless-test-*`
  (Apache-2.0, JUnit 6), not the deprecated commercial
  `vaadin-testbench-unit*`. This also settles part of the map's Licensing
  question for this Testing Tool.
- **Locating:** the lowest common denominator with the browser tools is: Java
  component type plus id, test id (`data-testid`), label, aria-label, text,
  placeholder, CSS class name, theme, attribute, or a scope to a parent or
  slot. CSS selectors and ARIA roles have no Browserless equivalent. Every
  Browserless query needs the component class, which a browser-side Testing
  Tool must map to a tag name.
- **Waiting:** Browserless has nothing to wait for. A Unified API wait or
  assertion-retry must work as a no-op (or one check) here. Background-thread
  work outside `UI.access()` and signals is not covered.
  `runPendingSignalsTasks()` has no browser counterpart.
- **Component API shape:** Browserless reads state from the real component
  (`getValue()` on the Java object). Browser tools read it from the DOM. The
  facade needs its own read methods. `selectItem` takes the display label.
  Grid cells hold real components only through `getCellComponent`.
- **Escape Hatch:** the natural Browserless Escape Hatch is the live
  `Component` object (`component()` / `getComponent()`) plus the tester. Both
  are server-side Java objects that have no meaning on the browser tools.
- **Unsupported features to record:** screenshots and visual checks, CSS and
  layout, client-side JavaScript (`executeJs` results), real keyboard and
  focus order beyond the simulation, browser navigation to external URLs,
  non-Flow views, renderer-created components found by query, and testing
  against a deployed URL.
- **App lifecycle:** Browserless owns the app lifecycle (it creates a mocked
  app in-process), whereas the browser tools need a running server. One suite
  on several Testing Tools needs a startup abstraction, or one setup per
  Testing Tool. `@ViewPackages` and Spring/Quarkus base classes are
  Browserless-specific.
- **Multi-user:** Browserless has first-class multi-user and multi-window
  contexts (`app.newUser().newWindow()`). This maps naturally to browser
  contexts and pages on Playwright.
