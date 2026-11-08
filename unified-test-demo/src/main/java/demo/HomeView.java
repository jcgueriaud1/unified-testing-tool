package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class HomeView extends VerticalLayout {
    public HomeView() {
        add(new H1("Home"), new Button("Go to profile", e -> getUI().ifPresent(ui -> ui.navigate(ProfileView.class))));
    }
}
