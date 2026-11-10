package com.vaadin.testing.unified;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteParameters;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/**
 * One browser tab or window showing the application. Injected into a Unified UI Test.
 * Escape Hatch types: Playwright {@code Page}, {@code BrowserlessUIContext}.
 */
public interface UnifiedPage extends Scope, HasEscapeHatch {

    // navigation: returns nothing, Auto-Waits until the view is shown and the server is idle
    void navigate(Class<? extends Component> view);
    <T, C extends Component & HasUrlParameter<T>> void navigate(Class<C> view, T parameter);
    void navigate(Class<? extends Component> view, RouteParameters parameters);
    /** Path relative to the base URL, may include a query string: {@code "profile?tab=2"}. */
    void navigate(String path);
    void reload();

    /** Polls the condition until true or the timeout; on Browserless flushes UI.access tasks and signals between polls. */
    void waitUntil(BooleanSupplier condition);

    void pressKey(String key);

    /** Needs {@link RequiresBrowser}. */
    void screenshot(Path file);

    /**
     * Whether this run's Testing Tool has a real browser: false on Browserless. For a test that runs on
     * every Testing Tool but does browser-only steps, such as {@link #screenshot}, only when it can.
     * A test that only makes sense in a browser declares {@link RequiresBrowser} instead.
     */
    boolean hasRealBrowser();

    // windows and users: all close at the end of the test
    /** Another window of the same user, in the same session. */
    UnifiedPage openWindow();

    /**
     * A window of another user, in a session of its own, logged in with these roles (Spring Security,
     * {@code ROLE_} prefix added). Starts on no view: navigate first.
     */
    UnifiedPage openAsUser(String username, String... roles);
}
