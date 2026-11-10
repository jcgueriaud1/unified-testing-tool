package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.Test;

@UnifiedSpringBootTest
class NavigationTest {

    @Test
    void navigatesByButton(UnifiedPage page) {
        page.navigate(HomeView.class);
        assertThat(page).isAt(HomeView.class);

        page.find(UnifiedButton.class).withText("Go to profile").single().click();

        assertThat(page).isAt(ProfileView.class).hasPath("profile");
    }

    @Test
    void navigatesByPathWithQuery(UnifiedPage page) {
        page.navigate("profile?tab=2");

        assertThat(page).isAt(ProfileView.class).hasPath("profile").hasQueryParameter("tab", "2");
    }

    /** A RouterLink navigates on the client only; isAt still sees the new location. */
    @Test
    void navigatesByRouterLink(UnifiedPage page) {
        page.navigate(HomeView.class);

        page.findComponent(RouterLink.class).withText("Reports").single().element().click();

        assertThat(page).isAt(ReportView.class).hasPath("report");
    }

    /** An error view has no route: isAt matches it by the view the page shows. */
    @Test
    void showsTheNotFoundView(UnifiedPage page) {
        page.navigate(HomeView.class);

        page.find(UnifiedButton.class).withText("Broken link").single().click();

        assertThat(page).isAt(NotFoundView.class).hasPath("no-such-page");
    }
}
