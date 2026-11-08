package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
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
}
