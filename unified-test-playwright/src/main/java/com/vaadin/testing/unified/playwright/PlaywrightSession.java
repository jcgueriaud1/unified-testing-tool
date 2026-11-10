package com.vaadin.testing.unified.playwright;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.internal.BrowserQueries;
import com.vaadin.testing.unified.internal.CurrentView;
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
import org.vaadin.addons.dramafinder.element.VaadinElement;
import org.vaadin.addons.dramafinder.element.query.ElementQueries;

/** One test on Playwright: one browser context and page. */
final class PlaywrightSession implements AdapterSession {

    private final BrowserContext context;
    private final Page page;
    private final String baseUrl;

    private final boolean ownsContext;
    private final java.util.List<PlaywrightSession> children = new ArrayList<>();

    private final double timeoutMillis;

    PlaywrightSession(BrowserContext context, Page page, String baseUrl, double timeoutMillis) {
        this(context, page, baseUrl, timeoutMillis, true);
    }

    private PlaywrightSession(BrowserContext context, Page page, String baseUrl, double timeoutMillis,
            boolean ownsContext) {
        this.context = context;
        this.page = page;
        this.baseUrl = baseUrl;
        this.timeoutMillis = timeoutMillis;
        this.ownsContext = ownsContext;
    }

    /** A new page in the same browser context: same cookies, so the same HTTP and Vaadin session. */
    @Override
    public AdapterSession openWindow() {
        Page second = context.newPage();
        return new PlaywrightSession(context, second, baseUrl, timeoutMillis, false);
    }

    /** A new browser context (own cookies, own session), logged in through the test-only endpoint. */
    @Override
    public AdapterSession openAsUser(String username, String... roles) {
        String loginUrl = LoginSupport.userUrl(baseUrl, username, roles);
        BrowserContext other = context.browser().newContext();
        other.setDefaultTimeout(timeoutMillis);
        PlaywrightSession session = new PlaywrightSession(other, other.newPage(), baseUrl, timeoutMillis, true);
        children.add(session);
        session.logIn(loginUrl);
        return session;
    }

    /** Opens the test-only login URL: the browser context's session belongs to that user afterwards. */
    void logIn(String loginUrl) {
        com.microsoft.playwright.Response response = page.navigate(loginUrl);
        if (response == null || !response.ok()) {
            throw new IllegalStateException("Test login failed: " + (response == null ? "no response" : response.status()
                    + " " + response.text()));
        }
    }

    Page page() {
        return page;
    }

    // ---------------------------------------------------------------- finding

    @Override
    public List<UnifiedElement> find(QuerySpec spec) {
        PlaywrightComponents.Support support = PlaywrightComponents.of(spec.componentType());
        Locator scope = spec.parent() == null ? null : ((PlaywrightElement) spec.parent()).locator();
        if (support.dramafinderElement() != null) {
            return findWithDramafinder(support, scope, spec.filters());
        }
        String tag = spec.tag() == null ? null : support.selector(spec.tag());
        if (tag == null) {
            throw new IllegalArgumentException(spec.componentType().getName()
                    + " has no @Tag; set @UnifiedComponent(tag = \"...\")");
        }
        // what CSS can express goes into the selector; the rest is checked in the page, for every candidate in
        // one round trip: no browser call per element, and no element detaching between two calls
        String selector = BrowserQueries.selector(tag, spec.filters());
        Locator candidates = scope == null ? page.locator(selector) : scope.locator(selector);
        List<Map<String, Object>> remaining = BrowserQueries.remaining(spec.filters());
        List<Integer> indices = new ArrayList<>();
        if (remaining.isEmpty()) {
            for (int i = 0, n = candidates.count(); i < n; i++) {
                indices.add(i);
            }
        } else {
            for (Object i : (List<?>) candidates.evaluateAll(BrowserQueries.MATCH, remaining)) {
                indices.add(((Number) i).intValue());
            }
        }
        List<UnifiedElement> result = new ArrayList<>();
        for (int i : indices) {
            Locator locator = candidates.nth(i);
            if (support.include(locator)) {
                result.add(new PlaywrightElement(this, locator, null, support));
            }
        }
        return result;
    }

    private List<UnifiedElement> findWithDramafinder(PlaywrightComponents.Support support, Locator scope,
            List<Filter> filters) {
        Class<? extends VaadinElement> type = support.dramafinderElement();
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
        return found.stream().filter(e -> support.include(e.getLocator()))
                .map(e -> (UnifiedElement) new PlaywrightElement(this, e.getLocator(), e, support)).toList();
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
        if (!CurrentView.hasRoute(view)) {
            return CurrentView.shows((String) page.evaluate(CurrentView.SCRIPT, baseUrl + CurrentView.PATH), view);
        }
        return currentPath().equals(strip(RouteNavigation.pathOf(view)));
    }

    /**
     * The browser's location, read in the page: {@code page.url()} only learns about a client-side navigation
     * (a RouterLink, {@code history.pushState}) on the next call to the browser.
     */
    private String location() {
        return (String) page.evaluate("() => location.href");
    }

    @Override
    public String currentPath() {
        String path = URI.create(location()).getPath();
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
        String query = URI.create(location()).getRawQuery();
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

    /** A full-page screenshot. */
    @Override
    public List<Path> writeFailureEvidence(Path directory, String name) throws java.io.IOException {
        java.nio.file.Files.createDirectories(directory);
        Path file = directory.resolve(name + ".png");
        page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
        return List.of(file);
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
        for (PlaywrightSession child : children) {
            child.close();
        }
        if (ownsContext) {
            context.close(); // closes the windows opened from this one too
        } else {
            page.close();
        }
    }

    static Pattern exactText(String text) {
        return Pattern.compile("^\\s*" + Pattern.quote(text) + "\\s*$");
    }
}
