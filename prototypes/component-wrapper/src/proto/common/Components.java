// PROTOTYPE: Core Set wrappers used by the scenario (Button, TextField, Notification, Dialog).
// Identical in all variants; variants only differ in HOW you find them (and scope from Dialog).
//
// Every action has Auto-Wait (decided in #8): waits until present/visible/enabled, acts,
// then waits for the server round-trip. Wrappers are lazy: each call re-resolves the component.
package proto.common;

public final class Components {
    private Components() {}

    /** Value-holding components. Getters do not retry; use UnifiedAssertions to wait for a value. */
    public interface HasValue<V> extends Wrapper {
        V getValue();
        void setValue(V value);   // types into the field like a user, then blurs (fires change)
        void clear();
    }

    /** Fields with a visible label, helper text and validation state. */
    public interface Field<V> extends HasValue<V> {
        String getLabel();
        String getHelperText();
        boolean isInvalid();
        String getErrorMessage();
        boolean isRequired();
    }

    public interface TextField extends Field<String> {
        String getPlaceholder();
    }

    public interface Button extends Wrapper {
        void click();
        String getText();
    }

    public interface Notification extends Wrapper {
        String getText();
        boolean isOpen();
    }

    public interface Dialog extends Wrapper {
        String getHeaderTitle();
        boolean isOpen();
        void close();             // like pressing Escape
    }
}
