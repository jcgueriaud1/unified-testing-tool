package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** "Approve" hides itself once used; it stays in the page, hidden. */
@AnonymousAllowed
@Route("approval")
public class ApprovalView extends VerticalLayout {

    public ApprovalView() {
        Span status = new Span("Waiting");
        status.setId("approval-status");
        Button approve = new Button("Approve");
        approve.addClickListener(e -> {
            status.setText("Approved");
            approve.setVisible(false);
        });
        add(status, approve);
    }
}
