// PROTOTYPE V1: "generated finders" (Browserless style).
// The entry object and every scope have one find method per Core Set type:
//   ui.findTextField().withLabel("Name").single()
// + discoverable: type "ui.find" and the IDE lists every component.
// - closed: a custom/add-on component can't add findMyComponent() to the entry object
//   (Browserless keeps its generator internal for this reason; see #18).
package proto.v1;

import java.util.List;
import java.util.Optional;
import proto.common.Components;
import proto.common.Filters.CommonFilters;
import proto.common.Filters.LabelFilter;
import proto.common.Filters.PlaceholderFilter;
import proto.common.Filters.TextFilter;
import proto.common.Wrapper;

public final class Api {
    private Api() {}

    /** Anything you can search inside: the entry object, a Dialog, a container. */
    public interface Scope {
        TextFieldQuery findTextField();
        ButtonQuery findButton();
        NotificationQuery findNotification();
        DialogQuery findDialog();
        ContainerQuery findContainer();   // untyped container for layouts (decided in #7)
    }

    /** The Unified API entry object, injected by @UnifiedUITest. Name still open. */
    public interface UI extends Scope {
        void navigate(String path);       // placeholder: "How does a test open a view?" (#16)
        <T> T unwrap(Class<T> type);      // test-level Escape Hatch: Page, WebDriver, BrowserlessUIContext
        <T> Optional<T> tryUnwrap(Class<T> type);
    }

    public interface Query<W, SELF> extends CommonFilters<SELF> {
        W single();            // lazy: exactly one match, checked when first used
        List<W> all();
        boolean exists();
    }

    public interface TextFieldQuery extends Query<TextField, TextFieldQuery>,
            LabelFilter<TextFieldQuery>, PlaceholderFilter<TextFieldQuery> {}

    public interface ButtonQuery extends Query<Components.Button, ButtonQuery>,
            TextFilter<ButtonQuery> {}

    public interface NotificationQuery extends Query<Components.Notification, NotificationQuery>,
            TextFilter<NotificationQuery> {}

    public interface DialogQuery extends Query<Dialog, DialogQuery> {
        DialogQuery withHeaderTitle(String title);
    }

    public interface ContainerQuery extends Query<Container, ContainerQuery> {}

    public interface TextField extends Components.TextField {}
    public interface Dialog extends Components.Dialog, Scope {}
    public interface Container extends Wrapper, Scope {}
}
