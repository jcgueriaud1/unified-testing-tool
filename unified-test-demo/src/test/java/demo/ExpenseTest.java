package demo;

import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedBigDecimalField;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedDatePicker;
import com.vaadin.testing.unified.component.UnifiedDateTimePicker;
import com.vaadin.testing.unified.component.UnifiedDetails;
import com.vaadin.testing.unified.component.UnifiedDiv;
import com.vaadin.testing.unified.component.UnifiedIntegerField;
import com.vaadin.testing.unified.component.UnifiedMenuBar;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedSection;
import com.vaadin.testing.unified.component.UnifiedTabs;
import com.vaadin.testing.unified.component.UnifiedTextArea;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.component.UnifiedUpload;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Core Set wrappers added for a typical Flow app, on every Testing Tool. */
@UnifiedSpringBootTest
class ExpenseTest {

    private static void assertStatus(UnifiedPage page, String text) {
        assertThat(page.findComponent(Span.class).withId("status").single()).hasText(text);
    }

    @Test
    void fillsTheValueFields(UnifiedPage page) {
        page.navigate(ExpenseView.class);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 8, 10, 30);

        page.find(UnifiedTextArea.class).withLabel("Notes").single().setValue("Taxi to the airport");
        page.find(UnifiedIntegerField.class).withLabel("Nights").single().setValue(3);
        page.find(UnifiedBigDecimalField.class).withLabel("Amount").single().setValue(new BigDecimal("54.20"));
        page.find(UnifiedDateTimePicker.class).withLabel("Departure").single().setValue(departure);
        page.find(UnifiedButton.class).withText("Save").single().click();

        assertThat(page.find(UnifiedNotification.class).single())
                .hasText("Saved: Taxi to the airport / 3 nights / 54.20 / 2026-11-08T10:30 / null /");
        assertThat(page.find(UnifiedTextArea.class).withLabel("Notes").single()).hasValue("Taxi to the airport");
        assertThat(page.find(UnifiedIntegerField.class).withLabel("Nights").single()).hasValue(3);
        assertThat(page.find(UnifiedBigDecimalField.class).withLabel("Amount").single()).hasValue(new BigDecimal("54.2"));
        assertThat(page.find(UnifiedDateTimePicker.class).withLabel("Departure").single()).hasValue(departure);
    }

    @Test
    void typesTextADatePickerCantParse(UnifiedPage page) {
        page.navigate(ExpenseView.class);
        UnifiedDatePicker returnDate = page.find(UnifiedDatePicker.class).withLabel("Return date").single();

        returnDate.typeText("dsdds");

        assertThat(returnDate).isInvalid();
        assertNull(returnDate.getValue());
    }

    @Test
    void selectsATab(UnifiedPage page) {
        page.navigate(ExpenseView.class);
        UnifiedTabs filter = page.find(UnifiedTabs.class).withTab("Approved").single();

        filter.selectTab("Rejected");

        assertThat(filter).hasTabs("Open", "Approved", "Rejected").hasSelectedTab("Rejected");
        assertStatus(page, "Showing Rejected");
    }

    @Test
    void opensDetails(UnifiedPage page) {
        page.navigate(ExpenseView.class);
        UnifiedDetails advanced = page.find(UnifiedDetails.class).withSummaryText("Advanced").single();

        advanced.open();
        advanced.content().find(UnifiedTextField.class).withLabel("Cost center").single().setValue("CC-7");

        assertThat(advanced).isOpen();
        page.find(UnifiedButton.class).withText("Save").single().click();
        assertThat(page.find(UnifiedNotification.class).single()).hasTextContaining("/ CC-7");
    }

    @Test
    void clicksNestedMenuItems(UnifiedPage page) {
        page.navigate(ExpenseView.class);
        UnifiedMenuBar menu = page.find(UnifiedMenuBar.class).withItem("File").single();

        assertEquals(List.of("File", "Help"), menu.getItems());
        menu.clickItem("File", "Export", "CSV");
        assertStatus(page, "Exported CSV");

        menu.clickItem("Help");
        assertStatus(page, "Help opened");
    }

    @Test
    void uploadsAFile(UnifiedPage page) {
        page.navigate(ExpenseView.class);

        page.find(UnifiedUpload.class).single().upload("receipt.txt", "text/plain",
                "Taxi 54.20".getBytes(StandardCharsets.UTF_8));

        assertStatus(page, "Received receipt.txt: Taxi 54.20");
    }

    @Test
    void searchesInPlainHtmlContainers(UnifiedPage page) {
        page.navigate(ExpenseView.class);

        assertEquals("Total120.50", page.find(UnifiedDiv.class).withClassName("summary").single().getText());
        assertEquals("120.50", page.find(UnifiedDiv.class).withClassName("summary").single()
                .findComponent(Span.class).withText("120.50").single().getText());
        assertThat(page.find(UnifiedSection.class).single()).hasText("Expenses are paid monthly.");
        // a component found with findComponent is a Scope too
        assertEquals(1, page.findComponent(VerticalLayout.class).single()
                .find(UnifiedSection.class).all().size());
    }
}
