package demo;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/** Key listeners, shortcuts and a custom DOM event. */
@Route("keyboard")
public class KeyboardView extends VerticalLayout {

    public KeyboardView() {
        // Enter in the field: a key listener on the field itself
        TextField search = new TextField("Search");
        search.addKeyDownListener(Key.ENTER, e -> Notification.show("Searching " + search.getValue()));

        // Ctrl+S anywhere in the view: a shortcut listening on the view
        Shortcuts.addShortcutListener(this, () -> Notification.show("Saved with Ctrl+S"), Key.KEY_S,
                KeyModifier.CONTROL).listenOn(this);

        // Enter in the comment field clicks Send: a click shortcut scoped to the form
        TextField comment = new TextField("Comment");
        Button send = new Button("Send", e -> Notification.show("Sent " + comment.getValue()));
        VerticalLayout form = new VerticalLayout(comment, send);
        send.addClickShortcut(Key.ENTER).listenOn(form);

        Button open = new Button("Open dialog", e -> {
            Dialog dialog = new Dialog(new Div("Press Escape to close"));
            dialog.setHeaderTitle("Escape me");
            dialog.open();
        });

        // a custom DOM event, as a web component or client-side script would fire it
        Div dropZone = new Div("Drop here");
        dropZone.setTestId("drop-zone");
        dropZone.getElement().addEventListener("item-dropped", e -> Notification.show("Item dropped"));

        add(search, form, open, dropZone);
    }
}
