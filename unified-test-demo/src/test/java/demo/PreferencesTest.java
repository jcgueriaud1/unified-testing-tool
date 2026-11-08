package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedCheckbox;
import com.vaadin.testing.unified.component.UnifiedDatePicker;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedNumberField;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Typed value fields; written once, runs on Browserless and Playwright. */
@UnifiedSpringBootTest
class PreferencesTest {

    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 5, 17);

    @Test
    void savesTypedValues(UnifiedPage page) {
        page.navigate(PreferencesView.class);

        UnifiedNumberField height = page.find(UnifiedNumberField.class).withLabel("Height (cm)").single();
        UnifiedDatePicker birthday = page.find(UnifiedDatePicker.class).withLabel("Birthday").single();
        UnifiedCheckbox newsletter = page.find(UnifiedCheckbox.class).withLabel("Subscribe to newsletter").single();

        height.setValue(180.5);
        birthday.setValue(BIRTHDAY);
        newsletter.check();
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single())
                .hasText("Saved: height 180.5, birthday 1990-05-17, newsletter yes, remember yes");
        assertThat(height).hasValue(180.5).isValid();
        assertThat(birthday).hasValue(BIRTHDAY).isValid();
        assertThat(newsletter).isChecked();
        assertEquals(180.5, height.getValue());
        assertEquals(BIRTHDAY, birthday.getValue());
    }

    @Test
    void wholeNumbers(UnifiedPage page) {
        page.navigate(PreferencesView.class);
        UnifiedNumberField height = page.find(UnifiedNumberField.class).withLabel("Height (cm)").single();

        height.setValue(170.0);

        assertThat(height).hasValue(170);
    }

    @Test
    void clearsValues(UnifiedPage page) {
        page.navigate(PreferencesView.class);
        UnifiedNumberField height = page.find(UnifiedNumberField.class).withLabel("Height (cm)").single();
        UnifiedDatePicker birthday = page.find(UnifiedDatePicker.class).withLabel("Birthday").single();
        height.setValue(180.0);
        birthday.setValue(BIRTHDAY);

        height.clear();
        birthday.clear();

        assertThat(height).isEmpty();
        assertThat(birthday).isEmpty();
        assertNull(height.getValue());
    }

    @Test
    void outOfRangeValuesAreInvalid(UnifiedPage page) {
        page.navigate(PreferencesView.class);
        UnifiedNumberField height = page.find(UnifiedNumberField.class).withLabel("Height (cm)").single();
        UnifiedDatePicker birthday = page.find(UnifiedDatePicker.class).withLabel("Birthday").single();

        height.setValue(300.0);
        birthday.setValue(LocalDate.of(2030, 1, 1));

        assertThat(height).isInvalid();
        assertThat(birthday).isInvalid();
    }

    @Test
    void readsNumberFieldLimits(UnifiedPage page) {
        page.navigate(PreferencesView.class);
        UnifiedNumberField height = page.find(UnifiedNumberField.class).withLabel("Height (cm)").single();

        assertEquals(50.0, height.getMin());
        assertEquals(250.0, height.getMax());
        assertEquals(0.5, height.getStep());
        assertThat(height).hasLabel("Height (cm)").isEmpty();
    }

    @Test
    void checksAndUnchecks(UnifiedPage page) {
        page.navigate(PreferencesView.class);
        UnifiedCheckbox remember = page.find(UnifiedCheckbox.class).withLabel("Remember me").single();
        UnifiedCheckbox newsletter = page.find(UnifiedCheckbox.class).withLabel("Subscribe to newsletter").single();

        assertThat(remember).isChecked();
        remember.check(); // already checked: no change
        assertThat(remember).isChecked();
        remember.uncheck();
        newsletter.setChecked(true);
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(remember).isNotChecked();
        assertThat(page.find(UnifiedNotification.class).single())
                .hasText("Saved: height null, birthday null, newsletter yes, remember no");
    }

    @Test
    void readsIndeterminate(UnifiedPage page) {
        page.navigate(PreferencesView.class);

        assertThat(page.find(UnifiedCheckbox.class).withLabel("Accept terms").single())
                .isIndeterminate().isNotChecked().hasLabel("Accept terms");
    }
}
