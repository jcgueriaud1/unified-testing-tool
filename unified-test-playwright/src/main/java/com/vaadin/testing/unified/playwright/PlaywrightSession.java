package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.Filter;
import com.vaadin.testing.unified.spi.QuerySpec;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.vaadin.addons.dramafinder.AutoWait;
import org.vaadin.addons.dramafinder.RouteNavigation;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;
import org.vaadin.addons.dramafinder.element.VaadinElement;
import org.vaadin.addons.dramafinder.element.query.ElementQueries;

/** One test on Playwright: one browser context and page. */
final class PlaywrightSession implements AdapterSession {

    /** Core Set components with a Dramafinder element: found through Dramafinder's query builder. */
    private static final Map<Class<? extends Component>, Class<? extends VaadinElement>> DRAMAFINDER = Map.of(
            TextField.class, TextFieldElement.class,
            Button.class, ButtonElement.class);

    /** Components whose visible part is not their own tag. */
    private static final Map<Class<? extends Component>, String> TAG_OVERRIDES = Map.of(
            Notification.class, "vaadin-notification-card[slot]");

    private final BrowserContext context;
    private final Page page;
    private final String baseUrl;

    PlaywrightSession(BrowserContext context, Page page, String baseUrl) {
        this.context = context;
        this.page = page;
        this.baseUrl = baseUrl;
    }

    Page page() {
        return page;
    }

    // ---------------------------------------------------------------- finding

    @Override
    public List<UnifiedElement> find(QuerySpec spec) {
        Class<? extends VaadinElement> dramafinderType = DRAMAFINDER.get(spec.componentType());
        Locator scope = spec.parent() == null ? null : ((PlaywrightElement) spec.parent()).locator();
        if (dramafinderType != null) {
            return findWithDramafinder(dramafinderType, scope, spec.filters());
        }
        String tag = TAG_OVERRIDES.getOrDefault(spec.componentType(), spec.tag());
        if (tag == null) {
            throw new IllegalArgumentException(spec.componentType().getName()
                    + " has no @Tag; set @UnifiedComponent(tag = \"...\")");
        }
        Locator candidates = scope == null ? page.locator(tag) : scope.locator(tag);
        List<UnifiedElement> result = new ArrayList<>();
        for (Locator locator : candidates.all()) {
            PlaywrightElement element = new PlaywrightElement(this, locator, null);
            if (spec.filters().stream().allMatch(f -> element.matches(f))) {
                result.add(element);
            }
        }
        return result;
    }

    private List<UnifiedElement> findWithDramafinder(Class<? extends VaadinElement> type, Locator scope,
            List<Filter> filters) {
        Object query = scope == null
                ? invokeStatic("find", new Class<?>[] { Page.class, Class.class }, page, type)
                : invokeStatic("find", new Class<?>[] { Locator.class, Class.class }, scope, type);
        for (Filter f : filters) {
            String method = switch (f.kind()) {
                case LABEL -> "withLabel";
                case ACCESSIBLE_NAME -> "withAccessibleName";
                case TEXT -> "withText";
                case PLACEHOLDER -> "withPlaceholder";
                case TEST_ID -> "withTestId";
                case ID -> "withId";
                case CLASS_NAME -> "withClassName";
                case THEME, ATTRIBUTE -> null;
            };
            if (f.kind() == Filter.Kind.THEME) {
                query = refine(query, l -> l.and(page.locator("[theme~=\"" + f.value() + "\"]")));
            } else if (f.kind() == Filter.Kind.ATTRIBUTE) {
                query = invoke(query, "withAttribute", new Class<?>[] { String.class, String.class }, f.name(), f.value());
            } else {
                String name = f.exact() || f.kind() == Filter.Kind.PLACEHOLDER ? method : method + "Containing";
                query = invoke(query, name, new Class<?>[] { String.class }, f.value());
            }
        }
        @SuppressWarnings("unchecked")
        List<VaadinElement> found = (List<VaadinElement>) invoke(query, "all", new Class<?>[0]);
        return found.stream().map(e -> (UnifiedElement) new PlaywrightElement(this, e.getLocator(), e)).toList();
    }

    private static Object refine(Object query, java.util.function.UnaryOperator<Locator> refinement) {
        return invoke(query, "refine", new Class<?>[] { java.util.function.UnaryOperator.class }, refinement);
    }

    private static Object invokeStatic(String name, Class<?>[] types, Object... args) {
        try {
            return ElementQueries.class.getMethod(name, types).invoke(null, args);
        } catch (InvocationTargetException e) {
            throw unwrapped(e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, types);
            return method.invoke(target, args);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(target.getClass().getSimpleName() + " has no " + name
                    + " filter in Dramafinder", e);
        } catch (InvocationTargetException e) {
            throw unwrapped(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RuntimeException unwrapped(InvocationTargetException e) {
        return e.getCause() instanceof RuntimeException r ? r : new IllegalStateException(e.getCause());
    }

    // ---------------------------------------------------------------- navigation

    @Override
    public void navigate(Class<? extends Component> view) {
        RouteNavigation.navigate(page, baseUrl, view);
    }

    @Override
    public void navigate(Class<? extends Component> view, Object urlParameter) {
        RouteNavigation.navigate(page, baseUrl, view, urlParameter);
    }

    @Override
    public void navigate(Class<? extends Component> view, RouteParameters parameters) {
        RouteNavigation.open(page, baseUrl, RouteNavigation.pathOf(view, parameters));
    }

    @Override
    public void navigate(String path) {
        if (path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            page.navigate(path);
            AutoWait.waitForVaadinIdle(page);
        } else {
            RouteNavigation.open(page, baseUrl, path.startsWith("/") ? path.substring(1) : path);
        }
    }

    @Override
    public void reload() {
        page.reload();
        AutoWait.waitForVaadinIdle(page);
    }

    @Override
    public boolean isAt(Class<? extends Component> view) {
        return currentPath().equals(strip(RouteNavigation.pathOf(view)));
    }

    @Override
    public String currentPath() {
        String path = URI.create(page.url()).getPath();
        String basePath = URI.create(baseUrl).getPath();
        if (basePath != null && !basePath.isEmpty() && path.startsWith(basePath)) {
            path = path.substring(basePath.length());
        }
        return strip(path);
    }

    private static String strip(String path) {
        String p = path == null ? "" : path;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    @Override
    public Optional<String> queryParameter(String name) {
        String query = URI.create(page.url()).getRawQuery();
        if (query == null) {
            return Optional.empty();
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (URLDecoder.decode(kv[0], StandardCharsets.UTF_8).equals(name)) {
                return Optional.of(kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
            }
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- the rest

    @Override
    public void sync() {
        // nothing to flush: the browser runs on its own
    }

    @Override
    public void pressKey(String key) {
        page.keyboard().press(key);
        AutoWait.afterAction(page);
    }

    @Override
    public void screenshot(Path file) {
        page.screenshot(new Page.ScreenshotOptions().setPath(file));
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        if (type.isInstance(page)) {
            return Optional.of(type.cast(page));
        }
        if (type.isInstance(context)) {
            return Optional.of(type.cast(context));
        }
        if (type.isInstance(context.browser())) {
            return Optional.of(type.cast(context.browser()));
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        context.close();
    }

    static Pattern exactText(String text) {
        return Pattern.compile("^\\s*" + Pattern.quote(text) + "\\s*$");
    }
}
