package demo.elsewhere;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import demo.HomeView;
import demo.ProfileView;
import org.junit.jupiter.api.Test;

/** A test outside the views' package: Browserless still finds every route of the Spring Boot app. */
@UnifiedSpringBootTest
class OtherPackageTest {

    @Test
    void navigatesToViewsOfTheApplication(UnifiedPage page) {
        page.navigate(HomeView.class);
        page.find(UnifiedButton.class).withText("Go to profile").single().click();

        assertThat(page).isAt(ProfileView.class);
    }
}
