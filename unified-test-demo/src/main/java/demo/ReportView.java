package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * A report filter. The year field has no visible label, only an accessible name ({@code setAriaLabel}). A
 * dialog stays in the page while closed (it is added to the layout); another one opens on demand.
 */
@AnonymousAllowed
@Route("report")
public class ReportView extends VerticalLayout {

    public ReportView() {
        ComboBox<String> year = new ComboBox<>();
        year.setItems("2025", "2026");
        year.setAriaLabel("Year");
        Span selected = new Span();
        selected.setId("selected-year");
        year.addValueChangeListener(e -> selected.setText(e.getValue()));

        Dialog help = new Dialog("Help");
        help.add(new Span("Pick a year."));
        add(help); // stays attached while closed

        Button export = new Button("Export", e -> {
            Dialog confirm = new Dialog("Export report");
            confirm.add(new Span("Export the report for " + year.getValue() + "?"));
            confirm.getFooter().add(new Button("Close", c -> confirm.close()));
            confirm.open();
        });
        add(year, selected, export);
    }
}
