package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedTabs;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

/** Tabs are addressed by their Item Name on every Testing Tool: the text of a tab holding a RouterLink. */
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
}
