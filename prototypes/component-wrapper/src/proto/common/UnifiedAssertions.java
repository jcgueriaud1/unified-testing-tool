// PROTOTYPE: retrying assertions (decided in #8). Each retries until the timeout (10 s default),
// then fails with expected and actual values. On Browserless, flushes UI.access/signals between tries.
// Same in all variants. Static-import style, one assertThat overload per wrapper type.
package proto.common;

import proto.common.Components.Button;
import proto.common.Components.Dialog;
import proto.common.Components.Notification;
import proto.common.Components.TextField;

public final class UnifiedAssertions {
    private UnifiedAssertions() {}

    public static TextFieldAssert assertThat(TextField field) { return null; }
    public static ButtonAssert assertThat(Button button) { return null; }
    public static NotificationAssert assertThat(Notification notification) { return null; }
    public static DialogAssert assertThat(Dialog dialog) { return null; }

    public interface WrapperAssert<SELF> {
        SELF isVisible();
        SELF isHidden();
        SELF isEnabled();
        SELF isDisabled();
    }

    public interface TextFieldAssert extends WrapperAssert<TextFieldAssert> {
        TextFieldAssert hasValue(String expected);
        TextFieldAssert isEmpty();
        TextFieldAssert isInvalid();
        TextFieldAssert isValid();
        TextFieldAssert hasErrorMessage(String expected);
        TextFieldAssert isRequired();
    }

    public interface ButtonAssert extends WrapperAssert<ButtonAssert> {
        ButtonAssert hasText(String expected);
    }

    public interface NotificationAssert extends WrapperAssert<NotificationAssert> {
        NotificationAssert isOpen();
        NotificationAssert hasText(String expected);
        NotificationAssert hasTextContaining(String expected);
    }

    public interface DialogAssert extends WrapperAssert<DialogAssert> {
        DialogAssert isOpen();
        DialogAssert isClosed();
        DialogAssert hasHeaderTitle(String expected);
    }
}
