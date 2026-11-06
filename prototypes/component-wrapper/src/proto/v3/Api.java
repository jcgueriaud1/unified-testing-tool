// PROTOTYPE V3 (chosen): class-literal lookups with per-type queries.
//   ui.find(UnifiedTextField.class).withLabel("Name").single()
// Each wrapper type names its own query type (Findable<Q>), so Java infers the query from the
// class literal and filters stay per type (ADR 0001).
// + open: a custom component's wrapper just declares `extends Findable<MyQuery>` (#18).
// + reads like TestBench's $(TextFieldElement.class); no constants to static-import.
// Wrappers use the Unified prefix: TextField, TextFieldElement (Dramafinder AND TestBench),
// TextFieldTester and TextFieldLocator (Browserless) are all taken, and unwrap() can bring them
// into the same file.
package proto.v3;

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

    /** Implemented by every wrapper type; Q is the query find() returns for it. */
    public interface Findable<Q> {}

    public interface Scope {
        <Q> Q find(Class<? extends Findable<Q>> type);
    }

    public interface UI extends Scope {
        void navigate(String path);          // placeholder: #16
        <T> T unwrap(Class<T> type);
        <T> Optional<T> tryUnwrap(Class<T> type);
    }

    public interface Query<W, SELF> extends CommonFilters<SELF> {
        W single();            // lazy: exactly one match, checked when first used
        List<W> all();
        boolean exists();
    }

    // --- wrappers ---

    public interface UnifiedTextField extends Components.TextField, Findable<TextFieldQuery> {}
    public interface UnifiedButton extends Components.Button, Findable<ButtonQuery> {}
    public interface UnifiedNotification extends Components.Notification, Findable<NotificationQuery> {}
    public interface UnifiedDialog extends Components.Dialog, Scope, Findable<DialogQuery> {}
    public interface UnifiedContainer extends Wrapper, Scope, Findable<ContainerQuery> {}

    // --- per-type queries ---

    public interface TextFieldQuery extends Query<UnifiedTextField, TextFieldQuery>,
            LabelFilter<TextFieldQuery>, PlaceholderFilter<TextFieldQuery> {}

    public interface ButtonQuery extends Query<UnifiedButton, ButtonQuery>, TextFilter<ButtonQuery> {}

    public interface NotificationQuery extends Query<UnifiedNotification, NotificationQuery>,
            TextFilter<NotificationQuery> {}

    public interface DialogQuery extends Query<UnifiedDialog, DialogQuery> {
        DialogQuery withHeaderTitle(String title);
    }

    public interface ContainerQuery extends Query<UnifiedContainer, ContainerQuery> {}
}
