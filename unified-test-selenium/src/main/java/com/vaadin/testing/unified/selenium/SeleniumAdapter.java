package com.vaadin.testing.unified.selenium;

import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchDriverProxy;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.spi.AdapterContext;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.TestingToolAdapter;
import java.util.Optional;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Selenium adapter on TestBench. One browser (driver) for the run's tests, wrapped by TestBench's driver
 * proxy, which waits for Vaadin before each command. Each test starts in a fresh tab with no cookies or
 * storage, so signed out and in a new session; {@code openAsUser} starts a browser of its own. TestBench checks its license when it loads: on CI, set the
 * {@code VAADIN_OFFLINE_KEY} environment variable.
 * <p>
 * Settings (JUnit configuration parameters or system properties):
 * <ul>
 * <li>{@code vaadin.testing.base-url}: app URL when the test has no Spring context; default
 * {@code http://localhost:8080}</li>
 * <li>{@code vaadin.testing.selenium.browser}: {@code chrome} (default) or {@code firefox}</li>
 * <li>{@code vaadin.testing.selenium.headless}: {@code true} (default)</li>
 * <li>{@code vaadin.testing.selenium.executable}: path to the browser binary, instead of the installed one</li>
 * <li>{@code vaadin.testing.selenium.reuse-browser}: {@code true} (default); {@code false} starts a new
 * browser for every test</li>
 * </ul>
 */
public final class SeleniumAdapter implements TestingToolAdapter {

    static final String BASE_URL = "vaadin.testing.base-url";
    static final String BROWSER = "vaadin.testing.selenium.browser";
    static final String HEADLESS = "vaadin.testing.selenium.headless";
    static final String EXECUTABLE = "vaadin.testing.selenium.executable";
    static final String REUSE_BROWSER = "vaadin.testing.selenium.reuse-browser";

    @Override
    public TestingTool tool() {
        return TestingTool.SELENIUM;
    }

    @Override
    public AdapterSession open(AdapterContext context) {
        String baseUrl = baseUrl(context);
        java.util.function.Supplier<TestBenchDriverProxy> browsers = () -> {
            TestBenchDriverProxy driver = TestBench.createDriver(newBrowser(context));
            driver.manage().timeouts().scriptTimeout(context.timeout());
            return driver;
        };
        SeleniumSession session = null;
        if (!"false".equalsIgnoreCase(context.configuration(REUSE_BROWSER).orElse("true"))) {
            SharedBrowser shared = context.extensionContext().getRoot()
                    .getStore(org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL)
                    .computeIfAbsent(SharedBrowser.class, key -> new SharedBrowser(browsers), SharedBrowser.class);
            TestBenchDriverProxy driver = shared.acquire(baseUrl);
            if (driver != null) { // null: in use by a test running in parallel
                session = new SeleniumSession(browsers, driver, shared::release, baseUrl, context.timeout());
            }
        }
        if (session == null) {
            session = new SeleniumSession(browsers, baseUrl, context.timeout());
        }
        // @WithMockUser / @WithUserDetails: log the browser in before the test body
        if (LoginSupport.isAvailable()) {
            LoginSupport.testUserUrl(baseUrl).ifPresent(session::logIn);
        }
        return session;
    }

    private static WebDriver newBrowser(AdapterContext context) {
        boolean headless = !"false".equalsIgnoreCase(context.configuration(HEADLESS).orElse("true"));
        Optional<String> executable = context.configuration(EXECUTABLE);
        if ("firefox".equals(context.configuration(BROWSER).orElse("chrome"))) {
            FirefoxOptions options = new FirefoxOptions();
            if (headless) {
                options.addArguments("-headless");
            }
            executable.ifPresent(options::setBinary);
            return new FirefoxDriver(options);
        }
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1280,1024");
        executable.ifPresent(options::setBinary);
        return new ChromeDriver(options);
    }

    private static String baseUrl(AdapterContext context) {
        Optional<String> fromSpring = SpringSupport.isAvailable()
                ? SpringSupport.baseUrl(context.extensionContext())
                : Optional.empty();
        String url = fromSpring.or(() -> context.configuration(BASE_URL)).orElse("http://localhost:8080");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }


    /**
     * The run's browser, closed by JUnit at the end of the run. A test takes it, and gives it back with one
     * blank tab and no cookies or storage left, so the next test starts signed out with fresh storage.
     */
    static final class SharedBrowser implements AutoCloseable {
        private final java.util.function.Supplier<TestBenchDriverProxy> browsers;
        private TestBenchDriverProxy driver;
        private String baseUrl;
        private boolean inUse;

        SharedBrowser(java.util.function.Supplier<TestBenchDriverProxy> browsers) {
            this.browsers = browsers;
        }

        synchronized TestBenchDriverProxy acquire(String baseUrl) {
            if (inUse) {
                return null;
            }
            if (driver == null) {
                driver = browsers.get();
            }
            this.baseUrl = baseUrl;
            inUse = true;
            return driver;
        }

        synchronized void release() {
            try {
                clean();
            } catch (RuntimeException e) {
                quit(); // a browser in a bad state is not reused: the next test starts a new one
            } finally {
                inUse = false;
            }
        }

        private void clean() {
            java.util.Set<String> old = driver.getWindowHandles();
            String fresh = driver.switchTo().newWindow(org.openqa.selenium.WindowType.TAB).getWindowHandle();
            for (String handle : old) {
                driver.switchTo().window(handle);
                driver.close();
            }
            driver.switchTo().window(fresh);
            Object wrapped = driver.getWrappedDriver();
            if (wrapped instanceof org.openqa.selenium.chromium.HasCdp cdp) {
                cdp.executeCdpCommand("Network.clearBrowserCookies", java.util.Map.of());
                cdp.executeCdpCommand("Storage.clearDataForOrigin",
                        java.util.Map.of("origin", origin(baseUrl), "storageTypes", "all"));
            } else {
                // other browsers: cookies and storage of the app's origin, from a page on it
                driver.get(baseUrl + "/robots.txt");
                driver.manage().deleteAllCookies();
                ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("try { localStorage.clear(); sessionStorage.clear(); } catch (e) {}");
                driver.get("about:blank");
            }
        }

        private static String origin(String url) {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getScheme() + "://" + uri.getAuthority();
        }

        private void quit() {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (RuntimeException ignored) {
                    // already gone
                }
                driver = null;
            }
        }

        @Override
        public synchronized void close() {
            quit();
        }
    }
}
