package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.TesterWrappers;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.testing.unified.UnifiedPage;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Escape Hatch to Browserless Test's test base API ({@code test(..)}, {@code $(..)}, {@code $view(..)},
 * {@code navigate(..)}, {@code fireShortcut(..)}, ...) on a Unified Page's window. The Browserless Migration
 * Recipe routes the calls it can't convert through it, so a migrated test keeps compiling and running on
 * Browserless:
 *
 * <pre>{@code
 * @Test
 * @RequiresTestingTool(TestingTool.BROWSERLESS)
 * void readsTheView(UnifiedPage page) {
 *     BrowserlessEscapeHatch browserless = BrowserlessEscapeHatch.of(page);
 *     ProfileView view = browserless.navigate(ProfileView.class);
 *     browserless.test(view.nameField).setValue("Alice");
 * }
 * }</pre>
 *
 * Browserless only. As inside any Escape Hatch, no Unified API guarantees apply: no Auto-Wait, no lazy lookup,
 * no retrying assertions. Every call makes the page's window the current one first, so the Escape Hatch and the
 * Unified API can be mixed in a test with several windows; a tester keeps working on the window its component
 * was found in. The {@code protected} methods of Browserless Test's base class are public here.
 */
public final class BrowserlessEscapeHatch implements TesterWrappers {

    private final UnifiedPage page;

    private BrowserlessEscapeHatch(UnifiedPage page) {
        this.page = page;
    }

    /** Browserless Test's test base API on the page's window; fails when the test does not run on Browserless. */
    public static BrowserlessEscapeHatch of(UnifiedPage page) {
        page.unwrap(BrowserlessUIContext.class);
        return new BrowserlessEscapeHatch(page);
    }

    /** The page's window, made the current one. */
    public BrowserlessUIContext context() {
        return page.unwrap(BrowserlessUIContext.class);
    }

    public <T extends Component> T navigate(Class<T> view) {
        return context().navigate(view);
    }

    public <C, T extends Component & HasUrlParameter<C>> T navigate(Class<T> view, C parameter) {
        return context().navigate(view, parameter);
    }

    public <T extends Component> T navigate(Class<T> view, Map<String, String> routeParameters) {
        return context().navigate(view, routeParameters);
    }

    public <T extends Component> T navigate(String location, Class<T> expectedView) {
        return context().navigate(location, expectedView);
    }

    public HasElement getCurrentView() {
        return context().getCurrentView();
    }

    public void fireShortcut(Key key, KeyModifier... modifiers) {
        context().fireShortcut(key, modifiers);
    }

    public <T extends ComponentTester<Y>, Y extends Component> T test(Y component) {
        return context().test(component);
    }

    public <T extends ComponentTester<Y>, Y extends Component> T test(Class<T> tester, Y component) {
        return context().test(tester, component);
    }

    public <T extends Component> ComponentQuery<T> find(Class<T> type) {
        return context().find(type);
    }

    public <T extends Component> ComponentQuery<T> find(Class<T> type, Component from) {
        return context().find(type, from);
    }

    public <T extends Component> ComponentQuery<T> findInView(Class<T> type) {
        return context().findInView(type);
    }

    public <T extends Component> ComponentQuery<T> $(Class<T> type) {
        return find(type);
    }

    public <T extends Component> ComponentQuery<T> $(Class<T> type, Component from) {
        return find(type, from);
    }

    public <T extends Component> ComponentQuery<T> $view(Class<T> type) {
        return findInView(type);
    }

    public void roundTrip() {
        context().roundTrip();
    }

    public boolean runPendingSignalsTasks() {
        return context().runPendingSignalsTasks();
    }

    public boolean runPendingSignalsTasks(long timeout, TimeUnit unit) {
        return context().runPendingSignalsTasks(timeout, unit);
    }
}
