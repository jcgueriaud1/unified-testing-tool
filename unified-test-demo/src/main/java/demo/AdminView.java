package demo;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

/** Only for admins. */
@RolesAllowed("ADMIN")
@Route("admin")
public class AdminView extends VerticalLayout {

    public AdminView(AuthenticationContext auth) {
        add(new H2("Admin area of " + auth.getPrincipalName().orElse("?")));
    }
}
