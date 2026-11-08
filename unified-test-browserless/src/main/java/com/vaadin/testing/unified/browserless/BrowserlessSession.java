package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.ComponentQuery;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.Filter;
import com.vaadin.testing.unified.spi.QuerySpec;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** One test on Browserless: one user, one window. */
final class BrowserlessSession implements AdapterSession {

    private final BrowserlessApplicationContext app;
    private final BrowserlessUIContext window;

    BrowserlessSession(BrowserlessApplicationContext app, BrowserlessUIContext window) {
        this.app = app;
        this.window = window;
    }

    BrowserlessUIContext window() {
        return window;
    }

    // ---------------------------------------------------------------- finding

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<UnifiedElement> find(QuerySpec spec) {
        Class<Component> type = (Class<Component>) spec.componentType();
        ComponentQuery<Component> query = spec.parent() == null
                ? window.find(type)
                : window.find(type, ((BrowserlessElement) spec.parent()).component());
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
        return (List) query.all().stream().map(c -> new BrowserlessElement(this, c)).toList();
    }

    // ---------------------------------------------------------------- navigation

    @Override
    public void navigate(Class<? extends Component> view) {
        window.navigate(view);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void navigate(Class<? extends Component> view, Object urlParameter) {
        window.navigate((Class) view, urlParameter);
    }

    @Override
    public void navigate(Class<? extends Component> view, RouteParameters parameters) {
        Map<String, String> map = new LinkedHashMap<>();
        parameters.getParameterNames().forEach(n -> map.put(n, parameters.get(n).orElse(null)));
        window.navigate(view, map);
    }

    @Override
    public void navigate(String path) {
        window.getUI().navigate(path.startsWith("/") ? path.substring(1) : path);
        window.roundTrip();
    }

    @Override
    public void reload() {
        throw new UnsupportedOperationException(
                "page.reload() is not implemented on Browserless yet (Unified API first version)");
    }

    @Override
    public boolean isAt(Class<? extends Component> view) {
        return view.isInstance(window.getCurrentView());
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
        return window.getUI().getInternals().getActiveViewLocation();
    }

    // ---------------------------------------------------------------- the rest

    @Override
    public void sync() {
        window.roundTrip();
        window.runPendingSignalsTasks(1, TimeUnit.MILLISECONDS);
    }

    @Override
    public void pressKey(String key) {
        throw new UnsupportedOperationException(
                "page.pressKey is not implemented on Browserless yet (Unified API first version)");
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
        UI ui = window.getUI();
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
            window.close();
        } finally {
            app.close();
        }
    }
}
