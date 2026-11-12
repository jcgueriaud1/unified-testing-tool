package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.stream.Collectors;

/** Selection fields; items are objects shown through an item label generator. */
@AnonymousAllowed
@Route("order")
public class OrderView extends VerticalLayout {

    public record Country(String code, String name) {}

    public enum Size { SMALL, MEDIUM, LARGE }

    public OrderView() {
        ComboBox<Country> country = new ComboBox<>("Country");
        country.setItems(List.of(new Country("FI", "Finland"), new Country("DE", "Germany"),
                new Country("FR", "France")));
        country.setItemLabelGenerator(Country::name);

        ComboBox<String> coupon = new ComboBox<>("Coupon", "SPRING", "SUMMER");
        coupon.setClearButtonVisible(true);
        coupon.setValue("SPRING");

        RadioButtonGroup<Size> size = new RadioButtonGroup<>("Size");
        size.setItems(Size.values());
        size.setItemLabelGenerator(s -> s.name().charAt(0) + s.name().substring(1).toLowerCase());
        size.setValue(Size.MEDIUM);
        size.setId("size");

        MultiSelectComboBox<String> toppings = new MultiSelectComboBox<>("Toppings");
        toppings.setItems("Cheese", "Ham", "Mushrooms", "Olives");

        Button order = new Button("Order", e -> Notification.show("Order: "
                + (country.getValue() == null ? "-" : country.getValue().code())
                + ", " + size.getValue()
                + ", " + toppings.getValue().stream().sorted().collect(Collectors.joining("+"))
                + ", coupon " + coupon.getValue()));

        add(country, coupon, size, toppings, order);
    }
}
