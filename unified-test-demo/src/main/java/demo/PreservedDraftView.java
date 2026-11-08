package demo;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Keeps what was typed on reload. */
@AnonymousAllowed
@Route("preserved-draft")
@PreserveOnRefresh
public class PreservedDraftView extends VerticalLayout {
    public PreservedDraftView() {
        add(new TextField("Draft"));
    }
}
