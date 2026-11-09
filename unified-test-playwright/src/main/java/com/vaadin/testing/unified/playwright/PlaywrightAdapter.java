package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.spi.AdapterContext;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.TestingToolAdapter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Playwright adapter on Dramafinder 2.0. One browser per test run (JVM), one fresh browser context and page
 * per test.
 * <p>
 * Settings (JUnit configuration parameters or system properties):
 * <ul>
 * <li>{@code vaadin.testing.base-url}: app URL when the test has no Spring context; default
 * {@code http://localhost:8080}</li>
 * <li>{@code vaadin.testing.playwright.browser}: {@code chromium} (default), {@code firefox}, {@code webkit}</li>
 * <li>{@code vaadin.testing.playwright.headless}: {@code true} (default)</li>
 * <li>{@code vaadin.testing.playwright.executable}: path to a browser executable, instead of Playwright's
 * own download</li>
 * </ul>
 */
public final class PlaywrightAdapter implements TestingToolAdapter {

    static final String BASE_URL = "vaadin.testing.base-url";
    static final String BROWSER = "vaadin.testing.playwright.browser";
    static final String HEADLESS = "vaadin.testing.playwright.headless";
    static final String EXECUTABLE = "vaadin.testing.playwright.executable";

    @Override
    public TestingTool tool() {
        return TestingTool.PLAYWRIGHT;
    }

    @Override
    public AdapterSession open(AdapterContext context) {
        ExtensionContext extension = context.extensionContext();
        Browser browser = extension.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
                .computeIfAbsent(SharedBrowser.class, key -> SharedBrowser.launch(context), SharedBrowser.class)
                .browser();
        String baseUrl = baseUrl(context);
        BrowserContext browserContext = browser.newContext();
        browserContext.setDefaultTimeout(context.timeout().toMillis()); // also for windows opened later
        Page page = browserContext.newPage();
        PlaywrightSession session = new PlaywrightSession(browserContext, page, baseUrl, context.timeout().toMillis());
        // @WithMockUser / @WithUserDetails: log the browser in before the test body
        if (LoginSupport.isAvailable()) {
            LoginSupport.testUserUrl(baseUrl).ifPresent(session::logIn);
        }
        return session;
    }

    private static String baseUrl(AdapterContext context) {
        Optional<String> fromSpring = SpringSupport.isAvailable()
                ? SpringSupport.baseUrl(context.extensionContext())
                : Optional.empty();
        String url = fromSpring.or(() -> context.configuration(BASE_URL)).orElse("http://localhost:8080");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Playwright and its browser, closed by JUnit at the end of the run. */
    record SharedBrowser(Playwright playwright, Browser browser) implements AutoCloseable {

        static SharedBrowser launch(AdapterContext context) {
            Playwright playwright = Playwright.create();
            BrowserType type = switch (context.configuration(BROWSER).orElse("chromium")) {
                case "firefox" -> playwright.firefox();
                case "webkit" -> playwright.webkit();
                default -> playwright.chromium();
            };
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(!"false".equalsIgnoreCase(context.configuration(HEADLESS).orElse("true")));
            context.configuration(EXECUTABLE).ifPresent(path -> options.setExecutablePath(Path.of(path))
                    .setArgs(List.of("--no-sandbox")));
            return new SharedBrowser(playwright, type.launch(options));
        }

        @Override
        public void close() {
            try {
                browser.close();
            } finally {
                playwright.close();
            }
        }
    }
}
