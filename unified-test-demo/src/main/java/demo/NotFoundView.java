package demo;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletResponse;

/** The app's own "not found" page: an error view, with no route of its own. */
@AnonymousAllowed
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    private final Paragraph path = new Paragraph();

    public NotFoundView() {
        add(new H1("Page not found"), path);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        path.setText("Nothing at /" + event.getLocation().getPath());
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
