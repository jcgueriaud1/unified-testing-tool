package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;

/** Golden migration of a TestBench base type, a test extending it and a page object (includePageObjects). */
@UnifiedSpringBootTest
abstract class AbstractProfileMigrated {

    void openProfile(UnifiedPage page) {
        open(page, "profile");
    }

    void open(UnifiedPage page, String path) {
        page.navigate(path);
    }
}
