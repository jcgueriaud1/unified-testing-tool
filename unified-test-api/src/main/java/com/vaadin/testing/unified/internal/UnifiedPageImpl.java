package com.vaadin.testing.unified.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.query.ComponentQuery;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** The injected {@link UnifiedPage}: delegates to the run's adapter session. */
public final class UnifiedPageImpl implements UnifiedPage {

    private final TestRun run;

    public UnifiedPageImpl(TestRun run) {
        this.run = run;
    }

    public TestRun run() {
        return run;
    }

    @Override
    public <Q> Q find(Class<? extends Findable<Q>> wrapperType) {
        return Wrappers.newQuery(run, wrapperType, null);
    }

    @Override
    public <C extends Component> ComponentQuery findComponent(Class<C> componentType) {
        return Wrappers.newComponentQuery(run, componentType, null);
    }

    @Override
    public void navigate(Class<? extends Component> view) {
        run.session().navigate(view);
    }

    @Override
    public <T, C extends Component & HasUrlParameter<T>> void navigate(Class<C> view, T parameter) {
        run.session().navigate(view, parameter);
    }

    @Override
    public void navigate(Class<? extends Component> view, RouteParameters parameters) {
        run.session().navigate(view, parameters);
    }

    @Override
    public void navigate(String path) {
        if (path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            run.requireBrowser("Navigating to an absolute URL");
        }
        run.session().navigate(path);
    }

    @Override
    public void reload() {
        run.session().reload();
    }

    @Override
    public void waitUntil(BooleanSupplier condition) {
        run.poll(() -> condition.getAsBoolean() ? Boolean.TRUE : null, () -> "Condition not met");
    }

    @Override
    public void pressKey(String key) {
        run.session().pressKey(key);
    }

    @Override
    public void screenshot(Path file) {
        run.requireBrowser("screenshot");
        run.session().screenshot(file);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return tryUnwrap(type).orElseThrow(() -> new UnsupportedOperationException(run.tool()
                + " can't provide " + type.getName() + ". If the test needs another Testing Tool, annotate it "
                + "with @RequiresTestingTool so it is skipped on " + run.tool() + "."));
    }

    @Override
    public <T> Optional<T> tryUnwrap(Class<T> type) {
        return run.session().unwrap(type);
    }

    @Override
    public String toString() {
        return "UnifiedPage[" + run.tool() + "]";
    }
}
