package com.vaadin.testing.unified.selenium;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.testbench.TestBenchDriverProxy;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.internal.CurrentView;
import com.vaadin.testing.unified.internal.RoutePaths;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.Filter;
import com.vaadin.testing.unified.spi.QuerySpec;
import com.vaadin.testing.unified.spi.StaleElementException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.interactions.Actions;

/**
 * One window of one browser on Selenium. The page injected into the test owns the driver; windows opened
 * from it are tabs of the same browser (same cookies, same session) and close with it.
 */
final class SeleniumSession implements AdapterSession {

    /** The browser and the window its commands currently go to, shared by the test's windows. */
    private static final class Browser {
        final TestBenchDriverProxy driver;
        String current;

        Browser(TestBenchDriverProxy driver) {
            this.driver = driver;
            this.current = driver.getWindowHandle();
        }
    }

    private final Browser browser;
    private final java.util.function.Supplier<TestBenchDriverProxy> browsers;
    private final List<SeleniumSession> children = new ArrayList<>();
    private final TestBenchDriverProxy driver;
    private final String baseUrl;
    private final Duration timeout;
    private final String handle;
    private final boolean ownsDriver;
    /** Instead of quitting the driver on close: gives a shared browser back. Null: quit it. */
    private final Runnable release;

    /** A page in a new browser from {@code browsers}; {@code openAsUser} starts its own browser the same way. */
    SeleniumSession(java.util.function.Supplier<TestBenchDriverProxy> browsers, String baseUrl, Duration timeout) {
        this(browsers, new Browser(browsers.get()), baseUrl, timeout, true, null);
    }

    /** A page in a browser shared by the run's tests: {@code release} gives it back when the test ends. */
    SeleniumSession(java.util.function.Supplier<TestBenchDriverProxy> browsers, TestBenchDriverProxy shared,
            Runnable release, String baseUrl, Duration timeout) {
        this(browsers, new Browser(shared), baseUrl, timeout, true, release);
    }

    private SeleniumSession(java.util.function.Supplier<TestBenchDriverProxy> browsers, Browser browser,
            String baseUrl, Duration timeout, boolean ownsDriver, Runnable release) {
        this(browsers, browser, baseUrl, timeout, browser.current, ownsDriver, release);
    }

    private SeleniumSession(java.util.function.Supplier<TestBenchDriverProxy> browsers, Browser browser,
            String baseUrl, Duration timeout, String handle, boolean ownsDriver, Runnable release) {
        this.browsers = browsers;
        this.browser = browser;
        this.driver = browser.driver;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.handle = handle;
        this.ownsDriver = ownsDriver;
        this.release = release;
    }

    /** The driver, switched to this session's window first: several windows can live in one test. */
    TestBenchDriverProxy driver() {
        if (!handle.equals(browser.current)) {
            driver.switchTo().window(handle);
            browser.current = handle;
        }
        return driver;
    }

    Duration timeout() {
        return timeout;
    }

    Object js(String script, Object... args) {
        return stale(() -> ((JavascriptExecutor) driver()).executeScript(script, args));
    }

    /** Runs a WebDriver call; an element that went stale meanwhile becomes the core's retryable exception. */
    static <T> T stale(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (StaleElementReferenceException e) {
            throw new StaleElementException("The element went stale (re-rendered by Vaadin)", e);
        }
    }

    static void stale(Runnable call) {
        stale(() -> {
            call.run();
            return null;
        });
    }

    /** Waits until Vaadin has handled every pending request (TestBench's waitForVaadin). */
    void waitForVaadin() {
        driver().getCommandExecutor().waitForVaadin();
    }

    /** The element as a TestBench element: what TestBench's own element classes wrap. */
    TestBenchElement testBench(WebElement element) {
        return element instanceof TestBenchElement tb ? tb
                : com.vaadin.testbench.TestBench.createElement(element, driver().getCommandExecutor());
    }

    // ---------------------------------------------------------------- finding

    @Override
    public List<UnifiedElement> find(QuerySpec spec) {
        SeleniumComponents.Support support = SeleniumComponents.of(spec.componentType());
        String tag = spec.tag() == null ? null : support.selector(spec.tag());
        if (tag == null) {
            throw new IllegalArgumentException(spec.componentType().getName()
                    + " has no @Tag; set @UnifiedComponent(tag = \"...\")");
        }
        // what CSS can express goes into the selector; the rest is checked in the page, for every candidate in
        // one round trip
        String selector = BrowserQueries.selector(tag, spec.filters());
        List<Map<String, Object>> remaining = BrowserQueries.remaining(spec.filters());
        List<SearchContext> roots = spec.parent() == null ? List.of(driver())
                : new ArrayList<>(((SeleniumElement) spec.parent()).roots());
        List<UnifiedElement> result = new ArrayList<>();
        try {
            for (SearchContext root : roots) {
                List<WebElement> candidates = new ArrayList<>();
                for (WebElement candidate : root.findElements(By.cssSelector(selector))) {
                    if (support.include(candidate, this)) {
                        candidates.add(candidate);
                    }
                }
                List<WebElement> matching = candidates;
                if (!remaining.isEmpty() && !candidates.isEmpty()) {
                    matching = new ArrayList<>();
                    // the rest of the filters check what the user sees, e.g. a notification's card
                    List<WebElement> contents = candidates.stream().map(c -> support.content(c, this)).toList();
                    // no TestBench filter on textContent or accessible name, nor in one round trip: #165, #21
                    for (Object i : (List<?>) js("return (" + BrowserQueries.MATCH + ")(arguments[0], arguments[1]);",
                            contents, remaining)) {
                        matching.add(candidates.get(((Number) i).intValue()));
                    }
                }
                for (WebElement candidate : matching) {
                    result.add(new SeleniumElement(this, candidate, support));
                }
            }
        } catch (StaleElementReferenceException | StaleElementException e) {
            // Vaadin re-rendered a candidate while it was checked: not ready yet, the core queries again
            throw new StaleElementException("An element went stale while finding " + selector, e);
        }
        return result;
    }

    // ---------------------------------------------------------------- navigation

    @Override
    public void navigate(Class<? extends Component> view) {
        open(RoutePaths.pathOf(view));
    }

    @Override
    public void navigate(Class<? extends Component> view, Object urlParameter) {
        open(RoutePaths.pathOf(view, urlParameter));
    }

    @Override
    public void navigate(Class<? extends Component> view, RouteParameters parameters) {
        open(RoutePaths.pathOf(view, parameters));
    }

    @Override
    public void navigate(String path) {
        if (path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            driver().get(path);
            waitForVaadin();
        } else {
            open(path.startsWith("/") ? path.substring(1) : path);
        }
    }

    private void open(String path) {
        driver().get(baseUrl + "/" + path);
        waitForVaadin();
    }

    @Override
    public void reload() {
        driver().navigate().refresh();
        waitForVaadin();
    }

    @Override
    public boolean isAt(Class<? extends Component> view) {
        if (!CurrentView.hasRoute(view)) {
            // the UI id from TestBench: #166
            Object answer = ((JavascriptExecutor) driver()).executeAsyncScript(
                    "const done = arguments[arguments.length - 1];"
                            + "(" + CurrentView.SCRIPT + ")(arguments[0]).then(done, () => done(null));",
                    baseUrl + CurrentView.PATH);
            return CurrentView.shows((String) answer, view);
        }
        return currentPath().equals(RoutePaths.pathOf(view));
    }

    @Override
    public String currentPath() {
        String path = URI.create(driver().getCurrentUrl()).getPath();
        String basePath = URI.create(baseUrl).getPath();
        if (basePath != null && !basePath.isEmpty() && path.startsWith(basePath)) {
            path = path.substring(basePath.length());
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Override
    public Optional<String> queryParameter(String name) {
        String query = URI.create(driver().getCurrentUrl()).getRawQuery();
        if (query == null) {
            return Optional.empty();
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (URLDecoder.decode(kv[0], StandardCharsets.UTF_8).equals(name)) {
                return Optional.of(kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
            }
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- windows

    /** A new tab of the same browser: same cookies, so the same HTTP and Vaadin session. */
    @Override
    public AdapterSession openWindow() {
        driver().switchTo().newWindow(WindowType.TAB);
        browser.current = driver.getWindowHandle();
        return new SeleniumSession(browsers, browser, baseUrl, timeout, browser.current, false, null);
    }

    /** A browser of its own (own cookies, own session), logged in through the test-only endpoint. */
    @Override
    public AdapterSession openAsUser(String username, String... roles) {
        String loginUrl = LoginSupport.userUrl(baseUrl, username, roles);
        SeleniumSession session = new SeleniumSession(browsers, new Browser(browsers.get()), baseUrl, timeout, true,
                null);
        children.add(session);
        session.logIn(loginUrl);
        return session;
    }

    /** Opens the test-only login URL: the browser's session belongs to that user afterwards. */
    void logIn(String loginUrl) {
        driver().get(loginUrl);
        String body = driver().findElement(By.tagName("body")).getText();
        if (!body.startsWith("Logged in as ")) {
            throw new IllegalStateException("Test login failed: " + body);
        }
    }

    // ---------------------------------------------------------------- the rest

    @Override
    public void sync() {
        // nothing to flush: the browser runs on its own
    }

    @Override
    public void pressKey(String key) {
        new Actions(driver()).sendKeys(SeleniumKeys.chord(key)).perform();
        waitForVaadin();
    }

    @Override
    public void screenshot(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, driver().getScreenshotAs(OutputType.BYTES));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** A screenshot of the window. */
    @Override
    public List<Path> writeFailureEvidence(Path directory, String name) throws java.io.IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(name + ".png");
        Files.write(file, driver().getScreenshotAs(OutputType.BYTES));
        return List.of(file);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        if (type.isInstance(driver())) {
            return Optional.of(type.cast(driver()));
        }
        Object wrapped = driver.getWrappedDriver();
        return type.isInstance(wrapped) ? Optional.of(type.cast(wrapped)) : Optional.empty();
    }

    @Override
    public void close() {
        for (SeleniumSession child : children) {
            child.close();
        }
        if (ownsDriver && release != null) {
            release.run(); // a shared browser: cleaned for the next test
        } else if (ownsDriver) {
            driver.quit(); // closes the windows opened from this one too
        } else {
            driver().close();
            browser.current = null;
        }
    }
}
