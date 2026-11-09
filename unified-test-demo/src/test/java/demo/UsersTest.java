package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;

/** Users and windows with Spring Security (spec 11-users.md). */
@UnifiedSpringBootTest
class UsersTest {

    // ---------------------------------------------------------------- both Testing Tools

    @Test
    void anonymousIsSentToLogin(UnifiedPage page) {
        page.navigate(AdminView.class);

        assertThat(page).isAt(LoginView.class);
    }

    @Test
    void openWindowSharesTheSession(UnifiedPage page) {
        page.navigate(WhoAmIView.class);
        assertThat(page.findComponent(Span.class).withText("Visits in this session: 1").single()).isVisible();

        UnifiedPage second = page.openWindow();
        second.navigate(WhoAmIView.class);

        assertThat(second.findComponent(Span.class).withText("Visits in this session: 2").single()).isVisible();
        assertThat(second.findComponent(Span.class).withText("Anonymous").single()).isVisible();
    }

    // ---------------------------------------------------------------- logged in: Browserless until #50

    @Test
    @WithMockUser(username = "alice", roles = "ADMIN")
    @RequiresTestingTool(TestingTool.BROWSERLESS) // Playwright: unified-testing-tool#50
    void adminSeesTheAdminArea(UnifiedPage page) {
        page.navigate(AdminView.class);

        assertThat(page).isAt(AdminView.class);
        assertThat(page.findComponent(H2.class).withText("Admin area of alice").single()).isVisible();
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void userWithoutTheRoleIsKeptOut(UnifiedPage page) {
        page.navigate(AdminView.class);

        assertEquals(0, page.findComponent(H2.class).withTextContaining("Admin area").all().size());
        page.navigate(WhoAmIView.class);
        assertThat(page.findComponent(Span.class).withText("Signed in as bob").single()).isVisible();
    }

    @Test
    @WithUserDetails("alice") // from the app's UserDetailsService
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    void userFromUserDetailsService(UnifiedPage page) {
        page.navigate(AdminView.class);

        assertThat(page).isAt(AdminView.class);
    }

    /** Run through the launcher only: on browser tools a test user must fail loudly, not run anonymous. */
    @UnifiedSpringBootTest
    @Tag("launched-only")
    static class LoggedInWithoutSkip {
        @Test
        @WithMockUser(username = "alice", roles = "ADMIN")
        void needsLogin(UnifiedPage page) {
            page.navigate(AdminView.class);
        }
    }

    @Test
    @RequiresTestingTool({ TestingTool.PLAYWRIGHT, TestingTool.SELENIUM })
    void browsersRefuseATestUserUntilLoginIsImplemented() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(LoggedInWithoutSkip.class))
                .configurationParameter("vaadin.testing.tool", System.getProperty("vaadin.testing.tool")).build();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);

        Throwable failure = listener.getSummary().getFailures().get(0).getException();
        assertTrue(failure.getMessage().contains("\"alice\"") && failure.getMessage().contains("#50"),
                failure.getMessage());
    }

    /** {@code @WithMockUser} on the class applies to every test in it. */
    @Nested
    @WithMockUser(username = "alice", roles = "ADMIN")
    @RequiresTestingTool(TestingTool.BROWSERLESS)
    class AsAlice {

        @Test
        void openAsUserIsAnotherSession(UnifiedPage alice) {
            UnifiedPage bob = alice.openAsUser("bob", "USER");
            alice.navigate(WhoAmIView.class);
            bob.navigate(WhoAmIView.class);

            assertThat(bob.findComponent(Span.class).withText("Signed in as bob").single()).isVisible();
            assertThat(bob.findComponent(Span.class).withText("Visits in this session: 1").single()).isVisible();
            // back to alice's window: still alice, own session
            assertThat(alice.findComponent(Span.class).withText("Signed in as alice").single()).isVisible();
            alice.navigate(AdminView.class);
            assertThat(alice).isAt(AdminView.class);
            bob.navigate(AdminView.class);
            assertEquals(0, bob.findComponent(H2.class).withTextContaining("Admin area").all().size());
        }

        @Test
        void openWindowKeepsTheUser(UnifiedPage alice) {
            UnifiedPage second = alice.openWindow();
            second.navigate(WhoAmIView.class);

            assertThat(second.findComponent(Span.class).withText("Signed in as alice").single()).isVisible();
        }
    }
}
