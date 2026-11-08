# Unified API for Vaadin UI tests

Write a Vaadin Flow UI test **once**, run it on **Browserless** (in the JVM, no browser) or on
**Playwright** through [Dramafinder](https://github.com/parttio/dramafinder) 2.0, chosen per run.

```java
@UnifiedSpringBootTest
class ProfileViewTest {

    @Test
    void savesName(UnifiedPage page) {
        page.navigate(ProfileView.class);

        UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
        name.setValue("Alice");
        page.find(UnifiedTextField.class).withLabel("Email").single().setValue("alice@example.com");
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single()).isOpen().hasText("Saved Alice");
        assertThat(name).hasValue("Alice").isValid();
    }
}
```

- **Specification:** [docs/spec](docs/spec/README.md). Decisions: the map [Unified testing API for Vaadin](https://github.com/jcgueriaud1/unified-testing-tool/issues/2), ADRs in [docs/adr](docs/adr), terms in [GLOSSARY.md](GLOSSARY.md).
- **Status:** first version. Spec draft, not yet `spec-v1`.

## Modules

| Module | Contains |
|---|---|
| `unified-test-api` | `@UnifiedUITest`, `UnifiedPage`, Wrappers (`UnifiedTextField`, `UnifiedButton`, `UnifiedNotification`, `UnifiedDialog`, `findComponent`), retrying `UnifiedAssertions`, Capabilities, Escape Hatch, adapter SPI |
| `unified-test-browserless` | Browserless Test adapter |
| `unified-test-playwright` | Playwright adapter on Dramafinder 2.0 |
| `unified-test-spring` | `@UnifiedSpringBootTest` |
| `unified-test-bom` | versions of the above and of the Testing Tools |
| `unified-test-demo` | Spring Boot + Vaadin demo app and its Unified UI Tests |

## Build and run

Java 21, Maven.

```
mvn install                                              # everything; demo tests on Browserless
mvn test -pl unified-test-demo -Dvaadin.testing.tool=playwright
```

The Playwright run uses Playwright's own browser download. To use an installed Chromium instead:

```
PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 mvn test -pl unified-test-demo \
    -Dvaadin.testing.tool=playwright \
    -Dvaadin.testing.playwright.executable=/path/to/chrome
```

## Settings

JUnit configuration parameters (`junit-platform.properties`) or system properties:

| Setting | Default |
|---|---|
| `vaadin.testing.tool` | `browserless` (or `playwright`) |
| `vaadin.testing.base-url` | from the Spring port, else `http://localhost:8080` |
| `vaadin.testing.timeout` | `10s` |
| `vaadin.testing.browserless.view-packages` | the test class's package |
| `vaadin.testing.playwright.browser` | `chromium` (or `firefox`, `webkit`) |
| `vaadin.testing.playwright.headless` | `true` |
| `vaadin.testing.playwright.executable` | Playwright's own browser |

## Not in this first version

| Area | State |
|---|---|
| Selenium (TestBench) adapter | not started (needs a TestBench license) |
| Rest of the Core Set (fields, selection fields, TabSheet) | in the spec, not implemented (tickets on the tracker) |
| Browserless: searching inside Grid row details (`row.details()`) | throws; toggling and checking details works |
| Browserless: `page.reload()`, `pressKey` (except Escape on a dialog), `dispatchEvent` | throw `UnsupportedOperationException` |
| `@ToolImplementation` overrides for add-ons | annotation exists, not honoured yet |
| Failure evidence (screenshot, component tree) | not captured yet |
