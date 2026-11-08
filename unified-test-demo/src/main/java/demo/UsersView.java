package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.stream.IntStream;

@Route("users")
public class UsersView extends VerticalLayout {

    public record User(String name, String email, String role) {}

    static final int USER_COUNT = 120;

    private String nameFilter = "";

    public UsersView() {
        // lazy: more rows than one page, so reading far rows needs scrolling and loading
        Grid<User> users = new Grid<>();
        users.setTestId("users");
        Grid.Column<User> name = users.addColumn(User::name).setHeader("Name").setKey("name");
        users.addColumn(User::email).setHeader("Email").setKey("email");
        users.addColumn(User::role).setHeader("Role").setKey("role");
        users.addComponentColumn(u -> new Button("Edit", e -> Notification.show("Editing " + u.name())))
                .setHeader("Actions");
        users.setItems(DataProvider.fromCallbacks(
                query -> filtered().skip(query.getOffset()).limit(query.getLimit()),
                query -> (int) filtered().count()));

        // a filter field in a second header row
        TextField filter = new TextField();
        filter.setPlaceholder("Filter by name");
        filter.setValueChangeMode(ValueChangeMode.EAGER);
        filter.addValueChangeListener(e -> {
            nameFilter = e.getValue();
            users.getDataProvider().refreshAll();
        });
        users.appendHeaderRow().getCell(name).setComponent(filter);

        // row details, opened by clicking the row (Flow's default)
        users.setItemDetailsRenderer(new ComponentRenderer<>(u -> new HorizontalLayout(
                new Span("Details of " + u.name()),
                new Button("Reset password", e -> Notification.show("Password reset for " + u.name())))));

        users.addItemClickListener(e -> Notification.show("Clicked " + e.getItem().name()));
        users.addItemDoubleClickListener(e -> Notification.show("Opened " + e.getItem().name()));

        // small, multi-select, sortable; the last column has no header text, only an id
        Grid<User> roles = new Grid<>();
        roles.setTestId("roles");
        roles.setSelectionMode(Grid.SelectionMode.MULTI);
        roles.addColumn(User::role).setHeader("Role").setKey("role").setSortable(true).setComparator(User::role);
        roles.addColumn(User::name).setId("owner");
        roles.setItems(List.of(new User("Ada", "", "Admin"), new User("Bob", "", "Editor"), new User("Cy", "", "Viewer")));
        roles.setAllRowsVisible(true);

        Grid<User> archived = new Grid<>();
        archived.setTestId("archived");
        archived.addColumn(User::name).setHeader("Name");
        archived.setItems(List.of());

        add(users, roles, archived);
    }

    private java.util.stream.Stream<User> filtered() {
        return IntStream.range(0, USER_COUNT).mapToObj(UsersView::user)
                .filter(u -> u.name().toLowerCase().contains(nameFilter.toLowerCase()));
    }

    static User user(int i) {
        int n = i + 1;
        return new User("User " + n, "user" + n + "@example.com", n % 10 == 0 ? "Admin" : "Member");
    }
}
