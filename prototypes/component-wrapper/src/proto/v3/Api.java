// PROTOTYPE V3: "type token" (closest to "find(TextField)" from #7, and to TestBench's $(X.class)).
// A typed constant per component selects the query type, so filters stay per type:
//   ui.find(TEXT_FIELD).withLabel("Name").single()
// + open: a custom component ships its own QueryType constant; no change to the entry object (#18).
// + one find() method, IDE completion after the token.
// - needs a static import of the token constants; a plain TextField.class can't work, because
//   Java erases Class<T> overloads, so the token type carries the query type instead.
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

    /** Selects which query (and therefore which filters and wrapper) find() returns. */
    public interface QueryType<Q> {}

    public static final QueryType<TextFieldQuery> TEXT_FIELD = null;
    public static final QueryType<ButtonQuery> BUTTON = null;
    public static final QueryType<NotificationQuery> NOTIFICATION = null;
    public static final QueryType<DialogQuery> DIALOG = null;
    public static final QueryType<ContainerQuery> CONTAINER = null;

    public interface Scope {
        <Q extends Query<?, Q>> Q find(QueryType<Q> type);
    }

    public interface UI extends Scope {
        void navigate(String path);          // placeholder: #16
        <T> T unwrap(Class<T> type);
        <T> Optional<T> tryUnwrap(Class<T> type);
    }

    public interface Query<W, SELF> extends CommonFilters<SELF> {
        W single();
        List<W> all();
        boolean exists();
    }

    public interface TextFieldQuery extends Query<TextField, TextFieldQuery>,
            LabelFilter<TextFieldQuery>, PlaceholderFilter<TextFieldQuery> {}

    public interface ButtonQuery extends Query<Components.Button, ButtonQuery>, TextFilter<ButtonQuery> {}

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
