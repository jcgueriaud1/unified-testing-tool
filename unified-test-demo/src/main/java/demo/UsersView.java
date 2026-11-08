package demo;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.stream.IntStream;

@Route("users")
public class UsersView extends VerticalLayout {

    public record User(String name, String email, String role) {}

    static final int USER_COUNT = 120;

    public UsersView() {
        // lazy: more rows than one page, so reading far rows needs scrolling and loading
        Grid<User> users = new Grid<>();
        users.setTestId("users");
        users.addColumn(User::name).setHeader("Name").setKey("name");
        users.addColumn(User::email).setHeader("Email").setKey("email");
        users.addColumn(User::role).setHeader("Role").setKey("role");
        users.setItems(DataProvider.fromCallbacks(
                query -> IntStream.range(query.getOffset(), Math.min(USER_COUNT, query.getOffset() + query.getLimit()))
                        .mapToObj(UsersView::user),
                query -> USER_COUNT));

        // small, with keys; the last column has no header text
        Grid<User> roles = new Grid<>();
        roles.setTestId("roles");
        roles.addColumn(User::role).setHeader("Role").setKey("role");
        roles.addColumn(User::name).setKey("owner");
        roles.setItems(List.of(new User("Ada", "", "Admin"), new User("Bob", "", "Editor"), new User("Cy", "", "Viewer")));
        roles.setAllRowsVisible(true);

        Grid<User> archived = new Grid<>();
        archived.setTestId("archived");
        archived.addColumn(User::name).setHeader("Name");
        archived.setItems(List.of());

        add(users, roles, archived);
    }

    static User user(int i) {
        int n = i + 1;
        return new User("User " + n, "user" + n + "@example.com", n % 10 == 0 ? "Admin" : "Member");
    }
}
