package demo;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Nested elements, a text node beside components, and components with a shadow DOM of their own. */
@AnonymousAllowed
@Route("tree")
public class TreeView extends VerticalLayout {

    public TreeView() {
        Div tree = new Div(new Span("one"), new Text("loose text"), new Div(new Span("nested")),
                new Button("Save", VaadinIcon.CHECK.create()), new FormLayout(new TextField("A")));
        tree.setTestId("tree");
        add(tree);
    }
}
