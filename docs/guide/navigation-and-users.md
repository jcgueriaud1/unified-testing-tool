# Navigation, users and windows

Spec: [3. Navigation](../spec/03-navigation.md), [11. Users](../spec/11-users.md).

## Opening a view

```java
page.navigate(ProfileView.class);                                // by route class (recommended)
page.navigate(OrderView.class, 42L);                             // HasUrlParameter
page.navigate(OrderView.class, new RouteParameters("id", "42")); // route template parameters
page.navigate("profile?tab=2");                                  // by path, relative to the base URL
page.reload();
```

`navigate` returns once the view is shown and the server is idle. The route class form survives renaming a route; use the path form for query parameters and routes registered at runtime.

## Checking where the page is

```java
--8<-- "unified-test-demo/src/test/java/demo/NavigationTest.java:navigatesByPathWithQuery"
```

`isAt(..)` also follows navigation the app does itself: a button that navigates, a `RouterLink`, a redirect to the login view:

```java
page.navigate(AdminView.class);
assertThat(page).isAt(LoginView.class);     // anonymous user sent to login
```

## Running as a user

With Spring Security, put Spring's own `@WithMockUser` or `@WithUserDetails` on the test or the class. On Browserless it sets the security context; on Playwright and Selenium the browser logs in through a test-only endpoint. The test is the same:

```java
@Test
@WithMockUser(username = "alice", roles = "ADMIN")
void adminSeesTheAdminArea(UnifiedPage page) {
    page.navigate(AdminView.class);

    assertThat(page).isAt(AdminView.class);
    assertThat(page.findComponent(H2.class).withText("Admin area of alice").single()).isVisible();
}
```

## Several windows and users

| Need | Write | Session |
|---|---|---|
| a second tab of the same user | `page.openWindow()` | shared |
| another user at the same time | `page.openAsUser("bob", "USER")` | its own |

```java
--8<-- "unified-test-demo/src/test/java/demo/UsersTest.java:openWindowSharesTheSession"
```

```java
UnifiedPage bob = alice.openAsUser("bob", "USER");   // starts on no view: navigate first
bob.navigate(WhoAmIView.class);
assertThat(bob.findComponent(Span.class).withText("Signed in as bob").single()).isVisible();
```

Each page is used on its own; there is no switching. All windows close at the end of the test.

## Spring beans

`@UnifiedSpringBootTest` is a `@SpringBootTest`, so the usual Spring test tools work, such as `@MockitoBean`:

```java
--8<-- "unified-test-demo/src/test/java/demo/MockedServiceTest.java:usesTheMock"
```

On Playwright and Selenium the app runs in a server thread, not in the test's thread: `@Transactional` on a test can't roll back what the app did there, so the Unified API refuses it on those tools.
