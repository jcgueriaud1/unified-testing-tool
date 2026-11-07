package com.vaadin.testing.unified.assertion;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedDialog;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;

/**
 * Retrying assertions: each check retries until the timeout (10 s default), then fails with the
 * expected and actual values. On Browserless, UI.access tasks and signals are flushed between tries.
 */
public final class UnifiedAssertions {
    private UnifiedAssertions() {}

    public static PageAssert assertThat(UnifiedPage page) { throw new UnsupportedOperationException("sketch"); }
    public static TextFieldAssert assertThat(UnifiedTextField field) { throw new UnsupportedOperationException("sketch"); }
    public static ButtonAssert assertThat(UnifiedButton button) { throw new UnsupportedOperationException("sketch"); }
    public static NotificationAssert assertThat(UnifiedNotification notification) { throw new UnsupportedOperationException("sketch"); }
    public static DialogAssert assertThat(UnifiedDialog dialog) { throw new UnsupportedOperationException("sketch"); }
    public static GridAssert assertThat(UnifiedGrid grid) { throw new UnsupportedOperationException("sketch"); }
    public static GridRowAssert assertThat(UnifiedGrid.UnifiedGridRow row) { throw new UnsupportedOperationException("sketch"); }
    public static GridColumnAssert assertThat(UnifiedGrid.UnifiedGridColumn column) { throw new UnsupportedOperationException("sketch"); }
    public static WrapperAssert<?> assertThat(UnifiedWrapper wrapper) { throw new UnsupportedOperationException("sketch"); }

    public interface PageAssert {
        PageAssert isAt(Class<? extends Component> view);
        PageAssert hasPath(String path);
        PageAssert hasQueryParameter(String name, String value);
    }

    public interface WrapperAssert<SELF> {
        SELF isVisible();
        SELF isHidden();
        SELF isEnabled();
        SELF isDisabled();
        SELF hasProperty(String name, Object value);
        /** Needs {@code @RequiresBrowser}. */
        SELF hasCssValue(String property, String value);
    }

    public interface TextFieldAssert extends WrapperAssert<TextFieldAssert> {
        TextFieldAssert hasValue(String expected);
        TextFieldAssert isEmpty();
        TextFieldAssert isValid();
        TextFieldAssert isInvalid();
        TextFieldAssert hasErrorMessage(String expected);
        TextFieldAssert isRequired();
    }

    public interface ButtonAssert extends WrapperAssert<ButtonAssert> {
        ButtonAssert hasText(String expected);
    }

    public interface NotificationAssert extends WrapperAssert<NotificationAssert> {
        NotificationAssert isOpen();
        NotificationAssert hasText(String expected);
        NotificationAssert hasTextContaining(String part);
    }

    public interface DialogAssert extends WrapperAssert<DialogAssert> {
        DialogAssert isOpen();
        DialogAssert isClosed();
        DialogAssert hasHeaderTitle(String expected);
    }

    public interface GridAssert extends WrapperAssert<GridAssert> {
        GridAssert hasRowCount(int expected);
        GridAssert isEmpty();
    }

    public interface GridRowAssert extends WrapperAssert<GridRowAssert> {
        GridRowAssert isSelected();
        GridRowAssert isNotSelected();
        GridRowAssert hasCellText(String columnHeader, String expected);
        GridRowAssert hasDetailsOpen();
    }

    public interface GridColumnAssert extends WrapperAssert<GridColumnAssert> {
        GridColumnAssert hasCellTexts(String... expected);
        GridColumnAssert isSortedAscending();
        GridColumnAssert isSortedDescending();
    }
}
