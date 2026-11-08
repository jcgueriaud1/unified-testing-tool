package com.vaadin.testing.unified.spi;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.testing.unified.UnifiedElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * One test on one Testing Tool. The core handles laziness, waiting and retries; the session finds
 * components as they are now and acts on them.
 */
public interface AdapterSession extends AutoCloseable {

    /** The components matching the spec right now, in document order. No waiting. */
    List<UnifiedElement> find(QuerySpec spec);

    void navigate(Class<? extends Component> view);
    void navigate(Class<? extends Component> view, Object urlParameter);
    void navigate(Class<? extends Component> view, RouteParameters parameters);
    void navigate(String path);
    void reload();

    boolean isAt(Class<? extends Component> view);
    /** Current path relative to the base URL, without leading slash or query string. */
    String currentPath();
    Optional<String> queryParameter(String name);

    /** Called between retries: lets pending server work run (Browserless: UI.access tasks and signals). */
    void sync();

    void pressKey(String key);
    void screenshot(Path file);

    /** The Testing Tool's own page-level object of the given type, if it has one. */
    <T> Optional<T> unwrap(Class<T> type);

    @Override
    void close();
}
