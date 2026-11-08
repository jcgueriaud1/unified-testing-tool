package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.BrowserlessUserContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import java.util.ArrayList;
import com.vaadin.browserless.ComponentQuery;
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
        children.add(session);
        return session;
    }

    // ---------------------------------------------------------------- finding

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<UnifiedElement> find(QuerySpec spec) {
        Class<Component> type = (Class<Component>) spec.componentType();
        if (spec.parent() == null) {
            return (List) applyFilters(window().find(type), type, spec).all().stream()
                    .map(c -> new BrowserlessElement(this, c)).toList();
        }
        // scoped: every root and its descendants (a root can itself be the match, e.g. a button in a grid cell)
        java.util.Set<Component> found = new java.util.LinkedHashSet<>();
        for (Component root : ((BrowserlessElement) spec.parent()).scopeRoots()) {
            if (type.isInstance(root) && root.getParent().isPresent()) {
                found.addAll(applyFilters(window().find(type, root.getParent().get()), type, spec)
                        .withCondition(c -> c == root).all());
            }
            found.addAll(applyFilters(window().find(type, root), type, spec).all());
        }
        return found.stream().map(c -> (UnifiedElement) new BrowserlessElement(this, c)).toList();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ComponentQuery<Component> applyFilters(ComponentQuery<Component> query, Class<Component> type, QuerySpec spec) {
        for (Filter f : spec.filters()) {
            query = switch (f.kind()) {
                case LABEL -> f.exact() ? query.withLabel(f.value())
                        : query.withCondition(c -> f.matches(c.getElement().getProperty("label")));
                case TEXT -> BrowserlessComponents.of(type).queryFindsText() && f.exact()
                        ? query.withText(f.value())
                        : query.withCondition(c -> f.matches(new BrowserlessElement(this, c).getText()));
                case PLACEHOLDER -> query.withPlaceholder(f.value());
                case TEST_ID -> query.withTestId(f.value());
                case ID -> query.withId(f.value());
                case CLASS_NAME -> query.withClassName(f.value());
                case THEME -> query.withTheme(f.value());
                case ATTRIBUTE -> query.withAttribute(f.name(), f.value());
                case ACCESSIBLE_NAME -> query.withCondition(c -> f.matches(BrowserlessElement.accessibleName(c)));
            };
        }
        return query;
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
            return Optional.of(type.cast(window));
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
