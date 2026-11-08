package demo;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Loses what was typed on reload: a new view instance. */
@AnonymousAllowed
@Route("draft")
public class DraftView extends VerticalLayout {
    public DraftView() {
        add(new TextField("Draft"));
    }
}
