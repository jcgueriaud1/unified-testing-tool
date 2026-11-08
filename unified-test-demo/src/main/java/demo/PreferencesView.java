package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;

/** A form with typed values: a number, a date and booleans. */
@Route("preferences")
public class PreferencesView extends VerticalLayout {

    public PreferencesView() {
        NumberField height = new NumberField("Height (cm)");
        height.setMin(50);
        height.setMax(250);
        height.setStep(0.5);

        DatePicker birthday = new DatePicker("Birthday");
        birthday.setMax(LocalDate.of(2026, 1, 1));

        Checkbox newsletter = new Checkbox("Subscribe to newsletter");
        Checkbox rememberMe = new Checkbox("Remember me", true);
        Checkbox terms = new Checkbox("Accept terms");
        terms.setIndeterminate(true);

        Button save = new Button("Save", e -> Notification.show("Saved: height " + height.getValue()
                + ", birthday " + birthday.getValue()
                + ", newsletter " + (newsletter.getValue() ? "yes" : "no")
                + ", remember " + (rememberMe.getValue() ? "yes" : "no")));

        add(height, birthday, newsletter, rememberMe, terms, save);
    }
}
