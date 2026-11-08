package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("profile")
public class ProfileView extends VerticalLayout {

    public ProfileView(ProfileService service) {
        TextField name = new TextField("Name");
        TextField email = new TextField("Email");
        email.setRequiredIndicatorVisible(true);
        TextField nickname = new TextField();
        nickname.setPlaceholder("Nickname");
        nickname.setTestId("nickname");

        Button save = new Button("Save", e -> {
            if (email.getValue().isBlank()) {
                email.setErrorMessage("Email is required");
                email.setInvalid(true);
                return;
            }
            email.setInvalid(false);
            Notification.show(service.save(name.getValue(), email.getValue()));
        });

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> Notification.show("Account deleted"));
        delete.setAriaLabel("Delete account");

        Button disabled = new Button("Archive");
        disabled.setEnabled(false);

        add(name, email, nickname, new HorizontalLayout(save, delete, disabled));
    }
}
