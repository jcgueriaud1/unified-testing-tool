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
 * Selenium adapter on TestBench. One browser (driver) per test, wrapped by TestBench's driver proxy, which
 * waits for Vaadin before each command. TestBench checks its license when it loads: on CI, set the
 * {@code VAADIN_OFFLINE_KEY} environment variable.
 * <p>
 * Settings (JUnit configuration parameters or system properties):
 * <ul>
 * <li>{@code vaadin.testing.base-url}: app URL when the test has no Spring context; default
 * {@code http://localhost:8080}</li>
 * <li>{@code vaadin.testing.selenium.browser}: {@code chrome} (default) or {@code firefox}</li>
 * <li>{@code vaadin.testing.selenium.headless}: {@code true} (default)</li>
 * <li>{@code vaadin.testing.selenium.executable}: path to the browser binary, instead of the installed one</li>
 * </ul>
 */
public final class SeleniumAdapter implements TestingToolAdapter {

    static final String BASE_URL = "vaadin.testing.base-url";
    static final String BROWSER = "vaadin.testing.selenium.browser";
    static final String HEADLESS = "vaadin.testing.selenium.headless";
    static final String EXECUTABLE = "vaadin.testing.selenium.executable";

    @Override
    public TestingTool tool() {
        return TestingTool.SELENIUM;
    }

    @Override
    public AdapterSession open(AdapterContext context) {
        failIfTestUserSet();
        String baseUrl = baseUrl(context);
        TestBenchDriverProxy driver = TestBench.createDriver(newBrowser(context));
        driver.manage().timeouts().scriptTimeout(context.timeout());
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1280, 1024));
        return new SeleniumSession(driver, baseUrl, context.timeout());
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

    /** Logging in on browser tools needs the login endpoint (#50): fail rather than run anonymous. */
    private static void failIfTestUserSet() {
        String user = testUser();
        if (user != null) {
            throw new UnsupportedOperationException("The test runs as \"" + user + "\" (@WithMockUser or "
                    + "@WithUserDetails): logging in on Selenium is not implemented yet (unified-testing-tool#50)."
                    + " Add @RequiresTestingTool(TestingTool.BROWSERLESS) for now.");
        }
    }

    private static String testUser() {
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object securityContext = holder.getMethod("getContext").invoke(null);
            Object auth = securityContext.getClass().getMethod("getAuthentication").invoke(securityContext);
            if (auth == null || auth.getClass().getSimpleName().equals("AnonymousAuthenticationToken")) {
                return null;
            }
            return String.valueOf(auth.getClass().getMethod("getName").invoke(auth));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static String baseUrl(AdapterContext context) {
        Optional<String> fromSpring = SpringSupport.isAvailable()
                ? SpringSupport.baseUrl(context.extensionContext())
                : Optional.empty();
        String url = fromSpring.or(() -> context.configuration(BASE_URL)).orElse("http://localhost:8080");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}
