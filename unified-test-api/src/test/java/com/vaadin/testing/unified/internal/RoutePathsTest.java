package com.vaadin.testing.unified.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import org.junit.jupiter.api.Test;

class RoutePathsTest {

    @Tag("div") @Route("profile") static class ProfileView extends Component {}
    @Tag("div") @Route static class OrderHistoryView extends Component {}
    @Tag("div") @Route static class MainView extends Component {}
    @Tag("div") @RoutePrefix("app") static class AppLayout extends Component implements RouterLayout {}
    @Tag("div") @RoutePrefix("admin") @ParentLayout(AppLayout.class) static class AdminLayout extends Component implements RouterLayout {}
    @Tag("div") @Route(value = "users", layout = AdminLayout.class) static class UsersView extends Component {}
    @Tag("div") @Route(value = "login", layout = AdminLayout.class, absolute = true) static class LoginView extends Component {}
    @Tag("div") @Route("order/:id/:tab?") static class OrderView extends Component {}

    @Test
    void routeValue() { assertEquals("profile", RoutePaths.pathOf(ProfileView.class)); }

    @Test
    void namingConvention() {
        assertEquals("orderhistory", RoutePaths.pathOf(OrderHistoryView.class));
        assertEquals("", RoutePaths.pathOf(MainView.class));
    }

    @Test
    void layoutPrefixes() {
        assertEquals("app/admin/users", RoutePaths.pathOf(UsersView.class));
        assertEquals("login", RoutePaths.pathOf(LoginView.class));
    }

    @Test
    void parameters() {
        assertEquals("profile/42", RoutePaths.pathOf(ProfileView.class, 42));
        assertEquals("order/7/items", RoutePaths.pathOf(OrderView.class, new RouteParameters(
                java.util.Map.of("id", "7", "tab", "items"))));
        assertEquals("order/7", RoutePaths.pathOf(OrderView.class, new RouteParameters("id", "7")));
    }
}
