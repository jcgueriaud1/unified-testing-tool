package demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import demo.addon.Badge;
import demo.addon.Rating;

/** Add-on components with wrappers of their own. */
@AnonymousAllowed
@Route("addon")
public class AddonView extends VerticalLayout {

    public AddonView() {
        Rating food = new Rating("Food");
        Rating service = new Rating("Service");
        service.setValue(4);

        Badge badge = new Badge();
        badge.setTestId("inbox");
        badge.setCount(2);
        Button receive = new Button("Receive", e -> badge.setCount(badge.getCount() + 1));

        add(food, service, badge, receive);
    }
}
