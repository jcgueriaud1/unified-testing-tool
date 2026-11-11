package demo;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * One row per expense with an icon-only actions menu ("⋮"): its trigger has no text, only an aria-label,
 * and every row's trigger looks the same. Deleting an approved expense is disabled.
 */
@AnonymousAllowed
@Route("expenses")
public class ExpenseListView extends VerticalLayout {

    private final Span status = new Span();

    public ExpenseListView() {
        status.setId("status");
        add(row("Taxi", false), row("Hotel", true), status);
    }

    private HorizontalLayout row(String expense, boolean approved) {
        MenuBar menu = new MenuBar();
        MenuItem trigger = menu.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        trigger.getElement().setAttribute("aria-label", "Actions for " + expense);
        trigger.getSubMenu().addItem("Edit", e -> status.setText("Editing " + expense));
        MenuItem delete = trigger.getSubMenu().addItem("Delete", e -> status.setText("Deleted " + expense));
        delete.setEnabled(!approved);
        return new HorizontalLayout(new Span(expense), menu);
    }
}
