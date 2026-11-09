package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;

/** A grid and an editor form: click a row, change the form, save. Used by docs/comparison.md. */
@AnonymousAllowed
@Route("people")
public class PeopleView extends HorizontalLayout {

    public static class Person {
        private String name;
        private String email;

        Person(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    private final Grid<Person> grid = new Grid<>();
    private final TextField name = new TextField("Name");
    private final TextField email = new TextField("Email");
    private final Button save = new Button("Save");
    private final Binder<Person> binder = new Binder<>();
    private Person edited;

    public PeopleView() {
        grid.addColumn(Person::getName).setHeader("Name");
        grid.addColumn(Person::getEmail).setHeader("Email");
        grid.setItems(List.of(
                new Person("Ada Lovelace", "ada@example.com"),
                new Person("Alan Turing", "alan@example.com"),
                new Person("Grace Hopper", "grace@example.com")));
        grid.addItemClickListener(e -> edit(e.getItem()));

        binder.forField(name).asRequired().bind(Person::getName, Person::setName);
        binder.forField(email).bind(Person::getEmail, Person::setEmail);

        save.setEnabled(false);
        save.addClickListener(e -> {
            if (binder.writeBeanIfValid(edited)) {
                grid.getDataProvider().refreshItem(edited);
                Notification.show("Saved " + edited.getName());
            }
        });

        add(grid, new VerticalLayout(name, email, save));
        setSizeFull();
    }

    private void edit(Person person) {
        edited = person;
        binder.readBean(person);
        save.setEnabled(true);
    }
}
