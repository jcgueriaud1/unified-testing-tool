package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** A TabSheet; two tabs have a field with the same label. */
@AnonymousAllowed
@Route("account")
public class AccountView extends VerticalLayout {

    public AccountView() {
        TabSheet tabs = new TabSheet();
        tabs.add("Profile", new VerticalLayout(new TextField("Name"), new TextField("Notes")));
        tabs.add("Security", new VerticalLayout(new TextField("Notes"),
                new Button("Change password", e -> Notification.show("Password changed"))));
        Tab billing = tabs.add("Billing", new VerticalLayout(new Button("Pay")));
        billing.setEnabled(false);
        tabs.addSelectedChangeListener(e -> Notification.show("Opened " + e.getSelectedTab().getLabel()));
        add(tabs);
    }
}
