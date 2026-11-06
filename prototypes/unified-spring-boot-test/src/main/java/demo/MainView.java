package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    public MainView(GreetingService service) {
        TextField name = new TextField("Name");
        Span greeting = new Span();
        greeting.setId("greeting");
        Button greet = new Button("Greet", e -> greeting.setText(service.greet(name.getValue())));
        add(name, greet, greeting);
    }
}
