// PROTOTYPE: Browserless adapter built from the test's Spring context, without SpringBrowserlessTest.
package demo.unified;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.Component;
import org.springframework.context.ApplicationContext;

final class BrowserlessPage implements UnifiedPage {
    private final BrowserlessApplicationContext app;
    private final BrowserlessUIContext window;

    BrowserlessPage(ApplicationContext spring, Class<?> viewPackageClass) {
        this.app = SpringBrowserlessApplicationContext.create(spring, viewPackageClass);
        this.window = app.newUser().newWindow();
    }

    @Override public void navigate(Class<? extends Component> view) { window.navigate(view); }
    @Override public void setTextField(String label, String value) { window.findTextField().withLabel(label).setValue(value); }
    @Override public void clickButton(String text) { window.findButton().withText(text).click(); }
    @Override public String textOf(String id) { return window.findSpan().withId(id).getText(); }
    @Override public TestingTool testingTool() { return TestingTool.BROWSERLESS; }
    @Override public String baseUrl() { return null; }
    @Override public void close() { app.close(); }
}
