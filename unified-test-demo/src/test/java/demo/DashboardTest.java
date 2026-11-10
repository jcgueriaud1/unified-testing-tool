package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

@UnifiedSpringBootTest
class DashboardTest {

    @Test
    void findsCardsByTheirWrapper(UnifiedPage page) {
        page.navigate(DashboardView.class);

        assertEquals(2, page.find(UnifiedMetricCard.class).all().size());
        assertEquals("12", page.find(UnifiedMetricCard.class).withTextContaining("Trips").single().getValue());
    }

    /** No tag: a browser can't find it, so no Testing Tool accepts the query. */
    @Test
    void refusesAQueryWithoutTag(UnifiedPage page) {
        page.navigate(DashboardView.class);

        assertThrows(IllegalArgumentException.class, () -> page.findComponent(Component.class));
    }
}
