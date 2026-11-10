package demo;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Metric cards among other vertical layouts (the view itself is one). */
@AnonymousAllowed
@Route("dashboard")
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        add(new HorizontalLayout(new MetricCard("Open reports", "4"), new MetricCard("Trips", "12")),
                new VerticalLayout());
    }
}
