package demo;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
import com.vaadin.flow.component.combobox.testbench.ComboBoxElement;
import com.vaadin.flow.component.combobox.testbench.MultiSelectComboBoxElement;
import com.vaadin.flow.component.datepicker.testbench.DatePickerElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.component.radiobutton.testbench.RadioButtonGroupElement;
import com.vaadin.flow.component.textfield.testbench.NumberFieldElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Golden migration of the Core Set fields: unified-test-rewrite turns FieldsTestBenchTest into FieldsMigratedTest. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "vaadin.testing.tool", matches = "selenium")
class FieldsTestBenchTest extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BrowserTest
    void savesPreferences() {
        getDriver().get("http://localhost:" + port + "/preferences");

        NumberFieldElement height = $(NumberFieldElement.class).withLabel("Height (cm)").single();
        height.setValue("170");
        DatePickerElement birthday = $(DatePickerElement.class).withLabel("Birthday").single();
        birthday.setDate(LocalDate.of(1990, 5, 17));
        CheckboxElement newsletter = $(CheckboxElement.class).withLabel("Subscribe to newsletter").single();
        newsletter.setChecked(true);
        $(ButtonElement.class).withCaption("Save").single().click();

        Assertions.assertEquals("170", height.getValue());
        Assertions.assertEquals(LocalDate.of(1990, 5, 17), birthday.getDate());
        Assertions.assertTrue(newsletter.isChecked());
        Assertions.assertEquals("Saved: height 170.0, birthday 1990-05-17, newsletter yes, remember yes",
                $(NotificationElement.class).waitForFirst().getText());
    }

    @BrowserTest
    void placesAnOrder() {
        getDriver().get("http://localhost:" + port + "/order");

        $(ComboBoxElement.class).withLabel("Country").single().selectByText("Germany");
        RadioButtonGroupElement size = $(RadioButtonGroupElement.class).withId("size").single();
        size.selectByText("Large");
        MultiSelectComboBoxElement toppings = $(MultiSelectComboBoxElement.class).withLabel("Toppings").single();
        toppings.selectByText("Ham");
        $(ButtonElement.class).withText("Order").single().click();

        Assertions.assertEquals("Large", size.getSelectedText());
        Assertions.assertEquals(List.of("Ham"), toppings.getSelectedTexts());
        Assertions.assertEquals("Order: DE, LARGE, Ham, coupon SPRING", $(NotificationElement.class).waitForFirst().getText());
    }
}
