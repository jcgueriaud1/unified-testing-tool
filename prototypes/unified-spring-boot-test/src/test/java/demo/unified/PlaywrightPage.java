// PROTOTYPE: Playwright adapter against the random port of the same Spring context.
package demo.unified;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import java.nio.file.Path;

final class PlaywrightPage implements UnifiedPage {
    private static final String VAADIN_IDLE = "() => !!window.Vaadin && !!window.Vaadin.Flow"
            + " && !!window.Vaadin.Flow.clients"
            + " && Object.values(window.Vaadin.Flow.clients).every(c => !c.isActive())";

    private final String baseUrl;
    private final Playwright playwright;
    private final Browser browser;
    private final Page page;

    PlaywrightPage(String baseUrl) {
        this.baseUrl = baseUrl;
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setExecutablePath(Path.of(System.getProperty("unified.chromium",
                        "/opt/pw-browsers/chromium-1243/chrome-linux64/chrome")))
                .setArgs(java.util.List.of("--no-sandbox")));
        this.page = browser.newPage();
        page.setDefaultTimeout(60_000);
    }

    private void waitForVaadin() { page.waitForFunction(VAADIN_IDLE); }

    @Override
    public void navigate(Class<? extends Component> view) {
        // static route resolution from @Route (#16); @RoutePrefix of parent layouts ignored here
        page.navigate(baseUrl + "/" + view.getAnnotation(Route.class).value());
        waitForVaadin();
    }

    @Override public void setTextField(String label, String value) {
        page.getByLabel(label, new Page.GetByLabelOptions().setExact(true)).fill(value);
        page.keyboard().press("Tab");   // blur: fires change
        waitForVaadin();
    }
    @Override public void clickButton(String text) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(text).setExact(true)).click();
        waitForVaadin();
    }
    @Override public String textOf(String id) { return page.locator("#" + id).textContent(); }
    @Override public TestingTool testingTool() { return TestingTool.PLAYWRIGHT; }
    @Override public String baseUrl() { return baseUrl; }
    @Override public void close() { browser.close(); playwright.close(); }
}
