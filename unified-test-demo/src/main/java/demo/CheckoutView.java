package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Two forms with fields of the same labels: tests tell them apart by their container. */
@AnonymousAllowed
@Route("checkout")
public class CheckoutView extends VerticalLayout {

    public CheckoutView() {
        TextField billingCity = new TextField("City");
        TextField shippingCity = new TextField("City");

        FormLayout billing = new FormLayout(new TextField("Street"), billingCity);
        billing.setTestId("billing");
        FormLayout shipping = new FormLayout(new TextField("Street"), shippingCity);
        shipping.setId("shipping");

        // a plain Div holding a layout: containers nest
        HorizontalLayout buttons = new HorizontalLayout(
                new Button("Save", e -> Notification.show("Billing: " + billingCity.getValue()
                        + ", shipping: " + shippingCity.getValue())),
                new Button("Cancel"));
        buttons.setTestId("buttons");
        Div footer = new Div(buttons);
        footer.setTestId("footer");

        add(billing, shipping, footer);
    }
}
