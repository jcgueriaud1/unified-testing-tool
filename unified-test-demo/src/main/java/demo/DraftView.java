package demo;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/** Loses what was typed on reload: a new view instance. */
@Route("draft")
public class DraftView extends VerticalLayout {
    public DraftView() {
        add(new TextField("Draft"));
    }
}
