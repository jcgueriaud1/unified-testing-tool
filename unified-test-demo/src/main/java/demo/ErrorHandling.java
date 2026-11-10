package demo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/** The app's error UI: an uncaught exception in a listener opens an error dialog. */
@Component
public class ErrorHandling implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(init -> init.getSession().setErrorHandler(error -> {
            UI ui = UI.getCurrent();
            if (ui == null) {
                return;
            }
            Dialog dialog = new Dialog("Something went wrong");
            dialog.add(new Paragraph(String.valueOf(error.getThrowable().getMessage())));
            dialog.open();
        }));
    }
}
