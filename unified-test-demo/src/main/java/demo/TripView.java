package demo;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Text that CSS changes on screen (uppercase, flex rows), inline styles, and many styled divs to filter. */
@AnonymousAllowed
@Route("trip")
public class TripView extends VerticalLayout {

    public TripView() {
        Span attention = new Span("Needs your attention");
        attention.setId("attention");
        attention.getStyle().set("text-transform", "uppercase");

        HorizontalLayout total = new HorizontalLayout(new Span("Trip total"), new Span("€54.00"));
        total.setId("total");
        total.getStyle().set("gap", "var(--lumo-space-m)").set("width", "180px");

        Div cards = new Div();
        for (int i = 1; i <= 60; i++) {
            Div card = new Div(new Span("Trip " + i));
            card.addClassName("trip-card");
            if (i == 42) {
                card.addClassName("selected");
            }
            cards.add(card);
        }
        add(attention, total, cards);
    }
}
