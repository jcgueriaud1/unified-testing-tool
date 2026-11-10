package demo;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** A card built from a common layout: in a browser it is one more {@code <vaadin-vertical-layout>}. */
public class MetricCard extends VerticalLayout {

    public MetricCard(String title, String value) {
        addClassName("metric-card");
        add(new Span(title), new Span(value));
    }
}
