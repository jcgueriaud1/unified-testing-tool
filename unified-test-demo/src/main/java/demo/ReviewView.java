package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Every click replaces the whole button bar with new buttons, as a view that re-renders its actions. */
@AnonymousAllowed
@Route("review")
public class ReviewView extends VerticalLayout {

    private final HorizontalLayout actions = new HorizontalLayout();
    private final Span status = new Span("Draft");

    public ReviewView() {
        status.setId("status");
        add(status, actions);
        show("Draft");
    }

    private void show(String state) {
        status.setText(state);
        actions.removeAll();
        switch (state) {
            case "Draft" -> actions.add(button("Submit", "Submitted"), button("Delete", "Deleted"));
            case "Submitted" -> actions.add(button("Approve", "Approved"), button("Reject", "Draft"),
                    button("Comment", "Submitted"));
            default -> actions.add(button("Reopen", "Draft"), new Button("Archive", e -> {
                throw new IllegalStateException("The archive is not available");
            }));
        }
    }

    private Button button(String text, String next) {
        return new Button(text, e -> show(next));
    }
}
