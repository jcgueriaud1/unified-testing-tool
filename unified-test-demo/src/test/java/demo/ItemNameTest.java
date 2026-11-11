package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.html.Span;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedMenuBar;
import com.vaadin.testing.unified.component.UnifiedTabs;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tabs and menu items are addressed by their Item Name on every Testing Tool: the text of a tab holding a
 * RouterLink, the aria-label of an icon-only menu item.
 */
@UnifiedSpringBootTest
class ItemNameTest {

    @Test
    void namesATabByTheTextOfTheLinkItHolds(UnifiedPage page) {
        page.navigate(RatesView.class);
        UnifiedTabs tabs = page.find(UnifiedTabs.class).withTab("Mileage Rates").single();

        assertThat(tabs).hasTabs("Allowance Rates", "Mileage Rates", "Lodging Rates")
                .hasSelectedTab("Allowance Rates");

        tabs.selectTab("Lodging Rates");

        assertThat(page.find(UnifiedTabs.class).single()).hasSelectedTab("Lodging Rates");
    }

    @Test
    void namesAnIconOnlyMenuItemByItsAriaLabel(UnifiedPage page) {
        page.navigate(ExpenseListView.class);
        UnifiedMenuBar taxi = page.find(UnifiedMenuBar.class).withItem("Actions for Taxi").single();

        assertEquals(List.of("Actions for Taxi"), taxi.getItems());
        assertEquals(List.of("Edit", "Delete"), taxi.getItems("Actions for Taxi"));

        taxi.clickItem("Actions for Taxi", "Edit");

        assertThat(page.findComponent(Span.class).withId("status").single()).hasText("Editing Taxi");
    }

    @Test
    void readsWhetherAMenuItemIsEnabled(UnifiedPage page) {
        page.navigate(ExpenseListView.class);
        UnifiedMenuBar taxi = page.find(UnifiedMenuBar.class).withItem("Actions for Taxi").single();
        UnifiedMenuBar hotel = page.find(UnifiedMenuBar.class).withItem("Actions for Hotel").single();

        assertTrue(hotel.isItemEnabled("Actions for Hotel"));
        assertTrue(hotel.isItemEnabled("Actions for Hotel", "Edit"));
        assertFalse(hotel.isItemEnabled("Actions for Hotel", "Delete"));
        assertTrue(taxi.isItemEnabled("Actions for Taxi", "Delete"));
    }
}
