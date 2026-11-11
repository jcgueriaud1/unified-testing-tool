# Unified API for Vaadin UI tests

Write a Vaadin Flow UI test **once**, run it on **Browserless** (in the JVM, no browser), on
**Playwright** through [Dramafinder](https://github.com/parttio/dramafinder) 2.0, or on **Selenium**
through TestBench, chosen per run.

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

- **Documentation site:** https://jcgueriaud1.github.io/unified-testing-tool/
- **Specification:** [docs/spec](docs/spec/README.md). Decisions: the map [Unified testing API for Vaadin](https://github.com/jcgueriaud1/unified-testing-tool/issues/2), ADRs in [docs/adr](docs/adr), terms in [GLOSSARY.md](GLOSSARY.md).
- **Status:** first version. Spec draft, not yet `spec-v1`.

## Modules

| Module | Contains |
|---|---|
| `unified-test-api` | `@UnifiedUITest`, `UnifiedPage`, Wrappers (`UnifiedTextField`, `UnifiedNumberField`, `UnifiedDatePicker`, `UnifiedCheckbox`, `UnifiedComboBox`, `UnifiedRadioButtonGroup`, `UnifiedMultiSelectComboBox`, `UnifiedButton`, `UnifiedNotification`, `UnifiedDialog`, `UnifiedGrid`, `UnifiedTabSheet`, `UnifiedContainer`, `findComponent`), users and windows (`@WithMockUser`, `openWindow`, `openAsUser`; browsers log in through a test-only endpoint), retrying `UnifiedAssertions`, Capabilities, Escape Hatch, adapter SPI |
| `unified-test-browserless` | Browserless Test adapter |
| `unified-test-playwright` | Playwright adapter on Dramafinder 2.0 |
| `unified-test-selenium` | Selenium adapter on TestBench 25.3 (needs a TestBench license) |
| `unified-test-spring` | `@UnifiedSpringBootTest`, test-only login endpoint for browsers |
| `unified-test-bom` | versions of the above and of the Testing Tools |
| `unified-test-demo` | Spring Boot + Vaadin demo app and its Unified UI Tests |
| `unified-test-rewrite` | OpenRewrite Migration Recipes from TestBench to Unified UI Tests ([ADR 0004](docs/adr/0004-test-migration-openrewrite-recipes-then-agent-skill.md)); not published before `spec-v1` |

## Try it

Alpha releases are on Maven Central as `org.vaadin.addons`, version `0.1.0-alpha2`. The API can still change between alphas. Import the BOM and add the adapters you run on as test dependencies:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.vaadin.addons</groupId>
            <artifactId>unified-test-bom</artifactId>
            <version>0.1.0-alpha2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.vaadin.addons</groupId>
        <artifactId>unified-test-spring</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.vaadin.addons</groupId>
        <artifactId>unified-test-browserless</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.vaadin.addons</groupId>
        <artifactId>unified-test-playwright</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

`unified-test-demo` shows a complete setup.

## Build and run

Java 21, Maven.

```
mvn install                                              # everything; demo tests on Browserless
mvn test -pl unified-test-demo -Dvaadin.testing.tool=playwright
VAADIN_OFFLINE_KEY=... mvn test -pl unified-test-demo -Dvaadin.testing.tool=selenium   # installed Chrome
```

TestBench checks its license when it loads: on CI, the `selenium` job reads the `VAADIN_OFFLINE_KEY` secret. Browserless and Playwright runs never load TestBench, even with the Selenium adapter on the classpath.

The Playwright run uses Playwright's own browser download. To use an installed Chromium instead:

```
PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 mvn test -pl unified-test-demo \
    -Dvaadin.testing.tool=playwright \
    -Dvaadin.testing.playwright.executable=/path/to/chrome
```

## Documentation site

The `Docs` workflow publishes `docs/`, `README.md` and `GLOSSARY.md` to GitHub Pages on every push to `main`. To preview it locally:

```
pip install 'mkdocs-material==9.*'
scripts/build-docs.sh        # builds target/site
```

## Settings

JUnit configuration parameters (`junit-platform.properties`) or system properties:

| Setting | Default |
|---|---|
| `vaadin.testing.tool` | `browserless` (or `playwright`, `selenium`) |
| `vaadin.testing.base-url` | from the Spring port, else `http://localhost:8080` |
| `vaadin.testing.timeout` | `10s` |
| `vaadin.testing.evidence-dir` | `target/surefire-reports/unified-evidence` |
| `vaadin.testing.browserless.view-packages` | in a Spring test, the `@SpringBootApplication`'s packages and the test's package; otherwise the test's package |
| `vaadin.testing.browserless.route-errors-to-session` | `false`; `true` sends an exception thrown by a listener to the session's `ErrorHandler`, as in a browser |
| `vaadin.testing.playwright.browser` | `chromium` (or `firefox`, `webkit`) |
| `vaadin.testing.playwright.headless` | `true` |
| `vaadin.testing.playwright.executable` | Playwright's own browser |
| `vaadin.testing.selenium.browser` | `chrome` (or `firefox`) |
| `vaadin.testing.selenium.headless` | `true` |
| `vaadin.testing.selenium.executable` | the installed browser |
| `vaadin.testing.selenium.reuse-browser` | `true`: one browser per run, a fresh tab without cookies or storage per test |

## Not in this first version

| Area | State |
|---|---|
| Browserless: searching inside Grid row details (`row.details()`) | throws; toggling and checking details works |
| Browserless: `page.reload()`, `pressKey`, `dispatchEvent` | simulated: events bubble from the target to the UI; `page.pressKey` goes to the focused component or the open dialog; client-side behaviour (e.g. typing text, Tab moving focus) doesn't happen ([#66](https://github.com/jcgueriaud1/unified-testing-tool/issues/66)) |
| Browserless: deselecting one item of a MultiSelectComboBox | done on the server, not as from the client ([#61](https://github.com/jcgueriaud1/unified-testing-tool/issues/61)) |
| Browser: a combo box's `getItems()` | the drop-down's first page only (50 items by default) |

## Cutting a release

Make sure the build passes locally and in the Tests workflow on `main`. Then, from an up-to-date `main` with push rights:

```
mvn release:prepare release:clean -DreleaseVersion=0.1.0-alpha2 -DdevelopmentVersion=0.1.0-SNAPSHOT
```

This runs the tests, sets the version in every module, commits, tags `unified-test-0.1.0-alpha2`, moves to the next snapshot and pushes. `release:perform` is not needed: the tag starts the Release workflow, which publishes every module except the demo to Maven Central (`mvn -Prelease deploy`, the same secrets as Dramafinder). Releases appear on Central within about half an hour.
