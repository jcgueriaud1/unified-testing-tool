// PROTOTYPE V2: "specs as values" (Playwright-locator feel).
// A spec describes what to find; scopes resolve it:
//   ui.find(TextField.withLabel("Name"))
// + specs are plain values: reusable constants in page objects, passable to helpers.
// + reads like a sentence; one find() method, so custom components plug in with their own Spec.
// - static factories live on the wrapper type, so the wrapper's simple name (TextField, Button)
//   collides with the Flow component class in Browserless-heavy test code.
package proto.v2;

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

    /** Describes a component to find; W is the wrapper it resolves to. */
    public interface Spec<W> {}

    public interface Scope {
        <W> W find(Spec<W> spec);            // lazy single(): exactly one match
        <W> List<W> findAll(Spec<W> spec);
        boolean exists(Spec<?> spec);
    }

    public interface UI extends Scope {
        void navigate(String path);          // placeholder: #16
        <T> T unwrap(Class<T> type);
        <T> Optional<T> tryUnwrap(Class<T> type);
    }

    // --- wrapper types carry the static entry points of their specs ---

    public interface TextField extends Components.TextField {
        static TextFieldSpec withLabel(String label) { return null; }
        static TextFieldSpec withAccessibleName(String name) { return null; }
        static TextFieldSpec withTestId(String testId) { return null; }
        static TextFieldSpec any() { return null; }
    }

    public interface Button extends Components.Button {
        static ButtonSpec withText(String text) { return null; }
        static ButtonSpec withAccessibleName(String name) { return null; }
        static ButtonSpec withTestId(String testId) { return null; }
        static ButtonSpec any() { return null; }
    }

    public interface Notification extends Components.Notification {
        static NotificationSpec withText(String text) { return null; }
        static NotificationSpec any() { return null; }
    }

    public interface Dialog extends Components.Dialog, Scope {
        static DialogSpec withHeaderTitle(String title) { return null; }
        static DialogSpec any() { return null; }
    }

    public interface Container extends Wrapper, Scope {
        static ContainerSpec withTestId(String testId) { return null; }
        static ContainerSpec withId(String id) { return null; }
    }

    // --- specs: per-type filters, chainable ---

    public interface TextFieldSpec extends Spec<TextField>, CommonFilters<TextFieldSpec>,
            LabelFilter<TextFieldSpec>, PlaceholderFilter<TextFieldSpec> {}

    public interface ButtonSpec extends Spec<Button>, CommonFilters<ButtonSpec>, TextFilter<ButtonSpec> {}

    public interface NotificationSpec extends Spec<Notification>, CommonFilters<NotificationSpec>,
            TextFilter<NotificationSpec> {}

    public interface DialogSpec extends Spec<Dialog>, CommonFilters<DialogSpec> {
        DialogSpec withHeaderTitle(String title);
    }

    public interface ContainerSpec extends Spec<Container>, CommonFilters<ContainerSpec> {}
}
