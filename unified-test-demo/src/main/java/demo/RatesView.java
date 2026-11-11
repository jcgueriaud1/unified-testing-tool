package demo;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;

/**
 * A tab bar of {@link RouterLink}s, the documented way to make tabs navigate: the tabs hold a link, not a
 * label. Each link opens this view again with the rate kind as parameter, which selects its tab.
 */
@AnonymousAllowed
@Route("rates")
public class RatesView extends VerticalLayout implements HasUrlParameter<String> {

    private static final List<String> KINDS = List.of("allowance", "mileage", "lodging");

    private final Tabs tabs = new Tabs(
            new Tab(new RouterLink("Allowance Rates", RatesView.class, "allowance")),
            new Tab(new RouterLink("Mileage Rates", RatesView.class, "mileage")),
            new Tab(new RouterLink("Lodging  Rates", RatesView.class, "lodging")));
    private final Span status = new Span();

    public RatesView() {
        status.setId("status");
        tabs.addSelectedChangeListener(e -> status.setText("Showing tab " + tabs.getSelectedIndex()));
        add(tabs, status);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String kind) {
        tabs.setSelectedIndex(kind == null ? 0 : Math.max(0, KINDS.indexOf(kind)));
    }
}
