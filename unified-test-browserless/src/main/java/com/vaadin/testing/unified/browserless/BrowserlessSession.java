package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.BrowserlessUserContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import java.util.ArrayList;
import com.vaadin.flow.component.Component;
import com.vaadin.browserless.internal.PrettyPrintTreeKt;
import com.vaadin.flow.component.UI;
import java.nio.file.Files;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.Filter;
import com.vaadin.testing.unified.spi.QuerySpec;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * One window of one user on Browserless. The page injected into the test owns the application; windows
 * and users opened from it are its children and close with it.
 */
final class BrowserlessSession implements AdapterSession {

    /** The window Browserless's thread-locals (current UI, session, security context) point to. */
    private static final ThreadLocal<BrowserlessSession> ACTIVE = new ThreadLocal<>();

    private final BrowserlessApplicationContext app;
    private final BrowserlessUserContext user;
    private final BrowserlessUIContext window;
    private final boolean ownsUser;
    private final boolean ownsApp;
    private final List<BrowserlessSession> children = new ArrayList<>();
    private boolean routeErrorsToSession;

    /** The test's own page: owns the app and the user. */
    BrowserlessSession(BrowserlessApplicationContext app, BrowserlessUserContext user) {
        this(app, user, true, true);
    }

    private BrowserlessSession(BrowserlessApplicationContext app, BrowserlessUserContext user, boolean ownsUser,
            boolean ownsApp) {
        this.app = app;
        this.user = user;
        this.window = user.newWindow();
        this.ownsUser = ownsUser;
        this.ownsApp = ownsApp;
        ACTIVE.set(this);
    }

    /** This window, made the current one first: several windows and users can live in one test. */
    BrowserlessUIContext window() {
        if (ACTIVE.get() != this) {
            window.activate();
            ACTIVE.set(this);
        }
        return window;
    }

    @Override
    public AdapterSession openWindow() {
        return child(new BrowserlessSession(app, user, false, false));
    }

    @Override
    public AdapterSession openAsUser(String username, String... roles) {
        if (!(app instanceof SecuredBrowserlessApplicationContext<?> secured)) {
            throw new IllegalStateException("openAsUser needs Spring Security in the application"
                    + " (spring-security on the test classpath and a SecurityFilterChain bean)");
        }
        return child(new BrowserlessSession(app, secured.newUser(username, roles), true, false));
    }

    private BrowserlessSession child(BrowserlessSession session) {
        session.routeErrorsToSession = routeErrorsToSession;
        children.add(session);
        return session;
    }

    /** {@code vaadin.testing.browserless.route-errors-to-session}: see {@link #act}. */
    BrowserlessSession routeErrorsToSession(boolean route) {
        this.routeErrorsToSession = route;
        return this;
    }

    /**
     * Runs a user action. In a browser, an exception thrown by an app listener goes to the session's
     * ErrorHandler (the app's error UI); Browserless Test throws it into the test. With
     * {@code route-errors-to-session}, it goes to the ErrorHandler as in a browser. Exceptions of Browserless
     * Test itself (a component that isn't usable) are always thrown.
     */
    void act(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            if (!routeErrorsToSession || thrownByBrowserless(e)) {
                throw e;
            }
            com.vaadin.flow.server.VaadinSession session = window().getUI().getSession();
            session.getErrorHandler().error(new com.vaadin.flow.server.ErrorEvent(e));
            window().roundTrip();
        }
    }

    private static boolean thrownByBrowserless(Throwable e) {
        for (StackTraceElement frame : e.getStackTrace()) {
            String type = frame.getClassName();
            if (type.startsWith("java.") || type.startsWith("jdk.") || type.startsWith("kotlin.")) {
                continue;
            }
            return type.startsWith("com.vaadin.browserless.") || type.startsWith("com.vaadin.testing.unified.");
        }
        return false;
    }

    // ---------------------------------------------------------------- finding

    /**
     * Matching components in document order, hidden ones included (as in a browser, where a component hidden
     * after it was shown stays in the page): Browserless Test's own queries skip them, so the adapter walks
     * the component tree itself.
     */
    @Override
    public List<UnifiedElement> find(QuerySpec spec) {
        List<Component> roots = spec.parent() == null ? List.of(window().getUI())
                : ((BrowserlessElement) spec.parent()).scopeRoots();
        java.util.Set<Component> found = new java.util.LinkedHashSet<>();
        for (Component root : roots) {
            found.addAll(findIn(root, spec, spec.parent() != null));
        }
        // a Grid's cell components are not in the component tree: add those the grid rendered for the client,
        // as a browser has them in the page
        for (Component cell : renderedGridCells(roots)) {
            found.addAll(findIn(cell, spec, true));
        }
        return found.stream().map(c -> (UnifiedElement) new BrowserlessElement(this, c)).toList();
    }

    /** Matching components under {@code root}, and {@code root} itself when {@code includeRoot}. */
    private List<Component> findIn(Component root, QuerySpec spec, boolean includeRoot) {
        BrowserlessComponents.Support support = BrowserlessComponents.of(spec.componentType());
        List<Component> found = new ArrayList<>();
        for (Component c : com.vaadin.browserless.internal.LocatorKt._walkAll(root)) {
            if ((c != root || includeRoot) && spec.componentType().isInstance(c) && support.include(c)
                    && spec.filters().stream().allMatch(f -> matches(c, f))) {
                found.add(c);
            }
        }
        return found;
    }

    private boolean matches(Component c, Filter f) {
        com.vaadin.flow.dom.Element e = c.getElement();
        return switch (f.kind()) {
            case LABEL -> f.exact() ? com.vaadin.browserless.ElementConditions.hasLabel(f.value()).test(c)
                    : f.matches(e.getProperty("label"));
            // getText(), as in a browser: Browserless's own withText reads the element's own text only
            case TEXT -> f.matches(new BrowserlessElement(this, c).getText());
            case PLACEHOLDER -> f.matches(e.getProperty("placeholder"));
            case TEST_ID -> f.value().equals(e.getAttribute("data-testid"));
            case ID -> f.value().equals(e.getAttribute("id"));
            case CLASS_NAME -> java.util.Arrays.stream(f.value().trim().split("\\s+"))
                    .allMatch(e.getClassList()::contains);
            case THEME -> {
                String theme = e.getAttribute("theme");
                yield theme != null && java.util.Arrays.asList(theme.split("\\s+")).contains(f.value());
            }
            case ATTRIBUTE -> f.value().equals(e.getAttribute(f.name()));
            case ACCESSIBLE_NAME -> f.matches(BrowserlessElement.accessibleName(c));
        };
    }

    /** How many rows of a grid are checked for rendered cell components, at most. */
    private static final int GRID_ROWS_CHECKED = 500;

    /**
     * The components that Grids under {@code roots} have rendered for the rows the client has, in their
     * component columns: what a browser has in the page. Grids inside those components count too.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<Component> renderedGridCells(List<Component> roots) {
        List<Component> cells = new ArrayList<>();
        java.util.Set<com.vaadin.flow.component.grid.Grid> grids = new java.util.LinkedHashSet<>();
        for (Component root : roots) {
            if (root instanceof com.vaadin.flow.component.grid.Grid grid) {
                grids.add(grid);
            }
            grids.addAll(window().find(com.vaadin.flow.component.grid.Grid.class, root).all());
        }
        for (com.vaadin.flow.component.grid.Grid<?> grid : grids) {
            List<com.vaadin.flow.component.grid.Grid.Column<?>> columns = (List) grid.getColumns().stream()
                    .filter(c -> c.isVisible() && c.getRenderer() instanceof com.vaadin.flow.data.renderer.ComponentRenderer)
                    .toList();
            if (columns.isEmpty()) {
                continue;
            }
            com.vaadin.flow.data.provider.DataCommunicator<Object> communicator =
                    (com.vaadin.flow.data.provider.DataCommunicator<Object>) grid.getDataCommunicator();
            com.vaadin.flow.component.grid.GridTester<?, Object> tester =
                    (com.vaadin.flow.component.grid.GridTester<?, Object>) (Object) window().test(grid);
            int size = Math.min(tester.size(), GRID_ROWS_CHECKED);
            for (int row = 0; row < size; row++) {
                Object item = tester.getRow(row);
                if (!communicator.isItemActive(item)) {
                    continue;
                }
                String key = communicator.getKeyMapper().key(item);
                for (com.vaadin.flow.component.grid.Grid.Column<?> column : columns) {
                    Component cell = com.vaadin.browserless.internal.RenderedComponentSupport
                            .getRenderedComponent(column, key);
                    if (cell != null) {
                        cells.add(cell);
                        cells.addAll(renderedGridCells(List.of(cell)));
                    }
                }
            }
        }
        return cells;
    }

    // ---------------------------------------------------------------- navigation

    @Override
    public void navigate(Class<? extends Component> view) {
        // UI.navigate, not BrowserlessUIContext.navigate: that one throws when the router lands elsewhere,
        // but a redirect (login, access denied) is a result the test asserts on
        window().getUI().navigate(view);
        window().roundTrip();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void navigate(Class<? extends Component> view, Object urlParameter) {
        window().getUI().navigate((Class) view, urlParameter);
        window().roundTrip();
    }

    @Override
    public void navigate(Class<? extends Component> view, RouteParameters parameters) {
        window().getUI().navigate(view, parameters);
        window().roundTrip();
    }

    @Override
    public void navigate(String path) {
        window().getUI().navigate(path.startsWith("/") ? path.substring(1) : path);
        window().roundTrip();
    }

    @Override
    public void reload() {
        UI ui = window().getUI();
        Location location = activeLocation();
        if (location == null) {
            return;
        }
        boolean preserved = ui.getInternals().getActiveRouterTargetsChain().stream()
                .anyMatch(target -> target.getClass().isAnnotationPresent(PreserveOnRefresh.class));
        if (!preserved) {
            // a browser creates the views again; with @PreserveOnRefresh it keeps them, as here
            ui.getInternals().getRouter().navigate(ui, location, NavigationTrigger.REFRESH, null, true, true);
        }
        window().roundTrip();
    }

    @Override
    public boolean isAt(Class<? extends Component> view) {
        return view.isInstance(window().getCurrentView());
    }

    @Override
    public String currentPath() {
        Location location = activeLocation();
        return location == null ? "" : location.getPath();
    }

    @Override
    public Optional<String> queryParameter(String name) {
        Location location = activeLocation();
        if (location == null) {
            return Optional.empty();
        }
        List<String> values = location.getQueryParameters().getParameters().get(name);
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    private Location activeLocation() {
        return window().getUI().getInternals().getActiveViewLocation();
    }

    // ---------------------------------------------------------------- the rest

    @Override
    public void sync() {
        window().roundTrip();
        window().runPendingSignalsTasks(1, TimeUnit.MILLISECONDS);
    }

    @Override
    public void pressKey(String key) {
        act(() -> pressPageKey(key));
    }

    private void pressPageKey(String key) {
        List<Dialog> open = window().find(Dialog.class).all().stream().filter(Dialog::isOpened).toList();
        BrowserlessElement.pressKey(this, BrowserlessKeys.pageTarget(window().getUI(), open), key);
    }

    /** The UI's component tree as text, with the location: what Browserless's TreeOnFailureExtension prints. */
    @Override
    public List<Path> writeFailureEvidence(Path directory, String name) throws java.io.IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(name + ".tree.txt");
        Location location = activeLocation();
        String tree = PrettyPrintTreeKt.toPrettyTree(window().getUI());
        Files.writeString(file, "Location: /" + (location == null ? "" : location.getPathWithQueryParameters())
                + "\n\n" + tree);
        return List.of(file);
    }

    @Override
    public void screenshot(Path file) {
        throw new UnsupportedOperationException("Browserless has no screenshots");
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        if (type.isInstance(window)) {
            // made current: the caller (BrowserlessEscapeHatch) works on the thread-locals of this window
            return Optional.of(type.cast(window()));
        }
        UI ui = window().getUI();
        if (type.isInstance(ui)) {
            return Optional.of(type.cast(ui));
        }
        if (type.isInstance(app)) {
            return Optional.of(type.cast(app));
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        try {
            for (int i = children.size() - 1; i >= 0; i--) {
                children.get(i).close();
            }
        } finally {
            if (ACTIVE.get() == this) {
                ACTIVE.remove();
            }
            try {
                window().close();
            } finally {
                if (ownsUser) {
                    user.close();
                }
                if (ownsApp) {
                    app.close();
                    ACTIVE.remove();
                }
            }
        }
    }
}
