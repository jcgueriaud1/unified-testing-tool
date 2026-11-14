package demo;

import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Golden migration of a test and its page object, with includePageObjects (GoldenMigrationTest). */
@SpringBootTest
class ProfileBrowserlessPageObjectTest extends SpringBrowserlessTest {

    @Test
    void savesTheProfile() {
        ProfileBrowserlessPage profile = new ProfileBrowserlessPage(this);
        profile.open();
        profile.save("Alice", "alice@example.com");

        Assertions.assertEquals("Saved Alice", test(profile.notification()).getText());
    }
}
