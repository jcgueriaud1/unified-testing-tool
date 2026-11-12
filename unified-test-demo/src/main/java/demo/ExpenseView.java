package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.UploadHandler;
import java.nio.charset.StandardCharsets;

/** An expense form with the fields, menus, tabs, details and upload a typical Flow app has. */
@AnonymousAllowed
@Route("expense")
public class ExpenseView extends VerticalLayout {

    public ExpenseView() {
        TextArea notes = new TextArea("Notes");
        IntegerField nights = new IntegerField("Nights");
        BigDecimalField amount = new BigDecimalField("Amount");
        DateTimePicker departure = new DateTimePicker("Departure");
        DatePicker returnDate = new DatePicker("Return date");

        Span status = new Span();
        status.setId("status");

        Tabs filter = new Tabs(new Tab("Open"), new Tab("Approved"), new Tab("Rejected"));
        filter.addSelectedChangeListener(e -> status.setText("Showing " + e.getSelectedTab().getLabel()));

        TextField costCenter = new TextField("Cost center");
        Details advanced = new Details("Advanced", costCenter);

        MenuBar menu = new MenuBar();
        MenuItem file = menu.addItem("File");
        MenuItem export = file.getSubMenu().addItem("Export");
        export.getSubMenu().addItem("PDF", e -> status.setText("Exported PDF"));
        export.getSubMenu().addItem("CSV", e -> status.setText("Exported CSV"));
        file.getSubMenu().addItem("Print", e -> status.setText("Printed"));
        menu.addItem("Help", e -> status.setText("Help opened"));

        Upload receipt = new Upload(UploadHandler.inMemory((metadata, bytes) -> status.setText(
                "Received " + metadata.fileName() + ": " + new String(bytes, StandardCharsets.UTF_8))));
        receipt.setId("receipt");

        // a single-shot "attach a file" upload: the list is cleared once the file has been handled
        Span attachments = new Span();
        attachments.setId("attachments");
        Upload attachment = new Upload();
        attachment.setId("attachment");
        attachment.setUploadHandler(UploadHandler.inMemory((metadata, bytes) -> {
            attachments.setText((attachments.getText().isEmpty() ? "" : attachments.getText() + ", ")
                    + metadata.fileName());
            attachment.clearFileList();
        }));

        Div summary = new Div(new Span("Total"), new Span("120.50"));
        summary.addClassName("summary");
        Section help = new Section(new Span("Expenses are paid monthly."));

        Button save = new Button("Save", e -> Notification.show("Saved: " + notes.getValue() + " / "
                + nights.getValue() + " nights / " + amount.getValue() + " / " + departure.getValue() + " / "
                + returnDate.getValue() + " / " + costCenter.getValue()));
        add(menu, filter, status, notes, nights, amount, departure, returnDate, advanced, receipt, attachment,
                attachments, summary, help, save);
    }
}
