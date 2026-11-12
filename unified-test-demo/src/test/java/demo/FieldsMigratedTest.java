package demo;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedCheckbox;
import com.vaadin.testing.unified.component.UnifiedComboBox;
import com.vaadin.testing.unified.component.UnifiedDatePicker;
import com.vaadin.testing.unified.component.UnifiedMultiSelectComboBox;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedNumberField;
import com.vaadin.testing.unified.component.UnifiedRadioButtonGroup;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

/** Golden migration of the Core Set fields: unified-test-rewrite turns FieldsTestBenchTest into FieldsMigratedTest. */
@UnifiedSpringBootTest
class FieldsMigratedTest {

    @Test
    void savesPreferences(UnifiedPage page) {
        page.navigate(PreferencesView.class);

        UnifiedNumberField height = page.find(UnifiedNumberField.class).withLabel("Height (cm)").single();
        height.setValue(170.0);
        UnifiedDatePicker birthday = page.find(UnifiedDatePicker.class).withLabel("Birthday").single();
        birthday.setValue(LocalDate.of(1990, 5, 17));
        UnifiedCheckbox newsletter = page.find(UnifiedCheckbox.class).withLabel("Subscribe to newsletter").single();
        newsletter.setChecked(true);
        // TODO unified: was withCaption("Save"); use withAccessibleName(..) for a button without text
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(height).hasValue(170.0);
        assertThat(birthday).hasValue(LocalDate.of(1990, 5, 17));
        assertThat(newsletter).isChecked();
        // TODO unified: was waitForFirst(); single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved: height 170.0, birthday 1990-05-17, newsletter yes, remember yes");
    }

    @Test
    void placesAnOrder(UnifiedPage page) {
        page.navigate(OrderView.class);

        page.find(UnifiedComboBox.class).withLabel("Country").single().selectItem("Germany");
        UnifiedRadioButtonGroup size = page.find(UnifiedRadioButtonGroup.class).withId("size").single();
        size.selectItem("Large");
        UnifiedMultiSelectComboBox toppings = page.find(UnifiedMultiSelectComboBox.class).withLabel("Toppings").single();
        toppings.selectItems("Ham");
        page.find(UnifiedButton.class).withText("Order").single().click();

        assertThat(size).hasSelectedItem("Large");
        assertThat(toppings).hasSelectedItems("Ham");
        // TODO unified: was waitForFirst(); single() fails when several components match
        assertThat(page.find(UnifiedNotification.class).single()).hasText("Order: DE, LARGE, Ham, coupon SPRING");
    }
}
