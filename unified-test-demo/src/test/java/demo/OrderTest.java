package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vaadin.testing.unified.RequiresTestingTool;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedComboBox;
import com.vaadin.testing.unified.component.UnifiedMultiSelectComboBox;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedRadioButtonGroup;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Selection fields; written once, runs on Browserless and Playwright. */
@UnifiedSpringBootTest
class OrderTest {

    // --8<-- [start:selectsByVisibleLabel]
    @Test
    void selectsByVisibleLabel(UnifiedPage page) {
        page.navigate(OrderView.class);
        UnifiedComboBox country = page.find(UnifiedComboBox.class).withLabel("Country").single();
        UnifiedRadioButtonGroup size = page.find(UnifiedRadioButtonGroup.class).withLabel("Size").single();
        UnifiedMultiSelectComboBox toppings = page.find(UnifiedMultiSelectComboBox.class).withLabel("Toppings").single();

        country.selectItem("Germany");
        size.selectItem("Large");
        toppings.selectItems("Olives", "Cheese");
        page.find(UnifiedButton.class).withText("Order").single().click();

        assertThat(page.find(UnifiedNotification.class).single())
                .hasText("Order: DE, LARGE, Cheese+Olives, coupon SPRING");
        assertThat(country).hasSelectedItem("Germany");
        assertThat(size).hasSelectedItem("Large");
        assertThat(toppings).hasSelectedItems("Cheese", "Olives");
    }
    // --8<-- [end:selectsByVisibleLabel]

    /** One group on the page: found without a filter, on every Testing Tool (#133). */
    @Test
    void findsTheOnlyRadioButtonGroupWithoutFilter(UnifiedPage page) {
        page.navigate(OrderView.class);
        assertThat(page.find(UnifiedRadioButtonGroup.class).single()).hasSelectedItem("Medium");
    }

    @Test
    void readsInitialSelection(UnifiedPage page) {
        page.navigate(OrderView.class);

        assertThat(page.find(UnifiedComboBox.class).withLabel("Country").single()).hasNoSelectedItem();
        assertThat(page.find(UnifiedComboBox.class).withLabel("Coupon").single()).hasSelectedItem("SPRING");
        assertThat(page.find(UnifiedRadioButtonGroup.class).withLabel("Size").single()).hasSelectedItem("Medium");
        assertThat(page.find(UnifiedMultiSelectComboBox.class).withLabel("Toppings").single()).hasNoSelectedItems();
        assertNull(page.find(UnifiedComboBox.class).withLabel("Country").single().getSelectedItem());
    }

    @Test
    void listsItems(UnifiedPage page) {
        page.navigate(OrderView.class);

        assertThat(page.find(UnifiedComboBox.class).withLabel("Country").single())
                .hasItems("Finland", "Germany", "France");
        assertThat(page.find(UnifiedRadioButtonGroup.class).withLabel("Size").single())
                .hasItems("Small", "Medium", "Large");
        assertEquals(List.of("Cheese", "Ham", "Mushrooms", "Olives"),
                page.find(UnifiedMultiSelectComboBox.class).withLabel("Toppings").single().getItems());
    }

    /** Every item, not only the first page the drop-down loads. */
    @Test
    @RequiresTestingTool({ TestingTool.BROWSERLESS, TestingTool.PLAYWRIGHT }) // Selenium: first page only (#127)
    void listsEveryItemOfALazyComboBox(UnifiedPage page) {
        page.navigate(OrderView.class);

        List<String> airports = page.find(UnifiedComboBox.class).withLabel("Airport").single().getItems();

        assertEquals(OrderView.AIRPORT_COUNT, airports.size());
        assertEquals("Airport 1", airports.get(0));
        assertEquals("Airport 120", airports.get(OrderView.AIRPORT_COUNT - 1));
    }

    @Test
    void deselectsAndClears(UnifiedPage page) {
        page.navigate(OrderView.class);
        UnifiedMultiSelectComboBox toppings = page.find(UnifiedMultiSelectComboBox.class).withLabel("Toppings").single();
        UnifiedComboBox coupon = page.find(UnifiedComboBox.class).withLabel("Coupon").single();
        UnifiedComboBox country = page.find(UnifiedComboBox.class).withLabel("Country").single();
        toppings.selectItems("Ham", "Mushrooms", "Olives");
        country.selectItem("France");

        toppings.deselectItems("Mushrooms", "Cheese"); // Cheese is not selected: ignored
        coupon.clear();   // clear button shown
        country.clear();  // no clear button

        assertThat(toppings).hasSelectedItems("Ham", "Olives");
        assertThat(coupon).hasNoSelectedItem();
        assertThat(country).hasNoSelectedItem();

        toppings.clear();
        page.find(UnifiedButton.class).withText("Order").single().click();
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Order: -, MEDIUM, , coupon null");
    }

    @Test
    void unknownItemFailsWithTheAvailableItems(UnifiedPage page) {
        page.navigate(OrderView.class);
        UnifiedComboBox country = page.find(UnifiedComboBox.class).withLabel("Country").single();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> country.selectItem("Spain"));

        assertEquals(true, e.getMessage().contains("[Finland, Germany, France]"), e.getMessage());
    }
}
