package com.vaadin.testing.unified.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import java.util.List;

/**
 * {@code isAt(view)} in a browser. A view with a route is matched by the browser's path. A view with no route
 * (an error view) is asked to the app: the test-only endpoint {@link #PATH} of {@code @UnifiedSpringBootTest}
 * returns the classes of the views the page's UI shows.
 */
public final class CurrentView {
    private CurrentView() {}

    /** Served by unified-test-spring, before the app's security. */
    public static final String PATH = "/__unified/current-view";

    /**
     * Browser script, argument: the endpoint's URL. Returns the endpoint's answer for the page's UI, or null
     * when the page has no Vaadin UI or the app doesn't serve the endpoint.
     */
    public static final String SCRIPT = """
            async (url) => {
              const clients = window.Vaadin && window.Vaadin.Flow ? Object.values(window.Vaadin.Flow.clients) : [];
              const client = clients.find(c => typeof c.getUIId === 'function');
              if (!client) return null;
              const response = await fetch(url + '?ui=' + client.getUIId(), {credentials: 'same-origin'});
              return response.ok ? await response.text() : null;
            }""";

    public static boolean hasRoute(Class<? extends Component> view) {
        return view.isAnnotationPresent(Route.class);
    }

    /**
     * Whether the endpoint's answer names {@code view}.
     *
     * @throws UnsupportedOperationException when there is no answer: the app doesn't serve the endpoint
     */
    public static boolean shows(String answer, Class<? extends Component> view) {
        if (answer == null) {
            throw new UnsupportedOperationException(view.getName() + " has no @Route, so isAt can't match it by the"
                    + " path. In a browser that needs @UnifiedSpringBootTest, which asks the app which view the page"
                    + " shows. Otherwise assert hasPath(..) and content that only this view renders.");
        }
        return List.of(answer.split("\n")).contains(view.getName());
    }
}
