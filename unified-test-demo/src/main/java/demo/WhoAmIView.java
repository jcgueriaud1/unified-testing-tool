package demo;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

/** Public: who is signed in, and how many times this session opened the view. */
@AnonymousAllowed
@Route("whoami")
public class WhoAmIView extends VerticalLayout {

    public WhoAmIView(AuthenticationContext auth) {
        VaadinSession session = VaadinSession.getCurrent();
        Integer visits = (Integer) session.getAttribute("visits");
        visits = visits == null ? 1 : visits + 1;
        session.setAttribute("visits", visits);

        Span user = new Span(auth.getPrincipalName().map(n -> "Signed in as " + n).orElse("Anonymous"));
        user.setTestId("user");
        Span count = new Span("Visits in this session: " + visits);
        count.setTestId("visits");
        add(user, count);
    }
}
