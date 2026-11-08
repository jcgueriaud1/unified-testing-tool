package com.vaadin.testing.unified.assertion;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.component.UnifiedDialog;
import com.vaadin.testing.unified.component.UnifiedGrid;
import com.vaadin.testing.unified.internal.grid.GridSupport;
import java.util.List;
import com.vaadin.testing.unified.component.UnifiedNotification;
import com.vaadin.testing.unified.component.UnifiedTextField;
import com.vaadin.testing.unified.internal.LazyElement;
import com.vaadin.testing.unified.internal.TestRun;
import com.vaadin.testing.unified.internal.UnifiedPageImpl;
import com.vaadin.testing.unified.internal.Wrappers;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Retrying assertions: each check retries until the timeout (10 s default), then fails with the
 * expected and actual values. On Browserless, UI.access tasks and signals are flushed between tries.
 */
public final class UnifiedAssertions {
    private UnifiedAssertions() {}

    public static PageAssert assertThat(UnifiedPage page) {
        if (!(page instanceof UnifiedPageImpl impl)) {
            throw new IllegalArgumentException("Not an injected UnifiedPage: " + page);
        }
        return new PageAssert(impl.run());
    }

    public static TextFieldAssert assertThat(UnifiedTextField field) { return new TextFieldAssert(field); }
    public static ButtonAssert assertThat(UnifiedButton button) { return new ButtonAssert(button); }
    public static NotificationAssert assertThat(UnifiedNotification notification) { return new NotificationAssert(notification); }
    public static DialogAssert assertThat(UnifiedDialog dialog) { return new DialogAssert(dialog); }
    public static GridAssert assertThat(UnifiedGrid grid) { return new GridAssert(grid); }
    public static GridRowAssert assertThat(UnifiedGrid.UnifiedGridRow row) { return new GridRowAssert(row); }
    public static GridColumnAssert assertThat(UnifiedGrid.UnifiedGridColumn column) { return new GridColumnAssert(column); }
    public static GenericAssert assertThat(UnifiedWrapper wrapper) { return new GenericAssert(wrapper); }

    // ---------------------------------------------------------------- shared retry

    static void check(TestRun run, String subject, String expectation, Object expected, Supplier<?> actual,
            Predicate<Object> ok) {
        Object[] last = { "<not available>" };
        run.poll(() -> {
            Object value = actual.get();
            last[0] = value;
            return ok.test(value) ? Boolean.TRUE : null;
        }, () -> "Expected " + subject + " " + expectation
                + (expected == null ? "" : " <" + expected + ">") + " but was <" + last[0] + ">");
    }

    // ---------------------------------------------------------------- page

    public static final class PageAssert {
        private final TestRun run;

        PageAssert(TestRun run) { this.run = run; }

        public PageAssert isAt(Class<? extends Component> view) {
            check(run, "the page", "to show", view.getSimpleName(),
                    () -> run.session().currentPath(), any -> run.session().isAt(view));
            return this;
        }

        public PageAssert hasPath(String path) {
            String expected = path.startsWith("/") ? path.substring(1) : path;
            check(run, "the page", "to have path", expected, () -> run.session().currentPath(), expected::equals);
            return this;
        }

        public PageAssert hasQueryParameter(String name, String value) {
            check(run, "query parameter " + name, "to be", value,
                    () -> run.session().queryParameter(name).orElse(null), v -> Objects.equals(v, value));
            return this;
        }
    }

    // ---------------------------------------------------------------- wrappers

    public abstract static class WrapperAssert<SELF extends WrapperAssert<SELF>> {
        final UnifiedWrapper wrapper;
        final LazyElement element;
        final TestRun run;

        WrapperAssert(UnifiedWrapper wrapper) {
            this.wrapper = wrapper;
            this.element = Wrappers.lazyElementOf(wrapper);
            this.run = element.run();
        }

        @SuppressWarnings("unchecked")
        SELF self() { return (SELF) this; }

        SELF check(String expectation, Object expected, Supplier<?> actual, Predicate<Object> ok) {
            UnifiedAssertions.check(run, element.describe(), expectation, expected, actual, ok);
            return self();
        }

        public SELF isVisible() { return check("to be visible", null, wrapper::isVisible, Boolean.TRUE::equals); }
        public SELF isHidden() { return check("to be hidden", null, wrapper::isVisible, Boolean.FALSE::equals); }
        public SELF isEnabled() { return check("to be enabled", null, wrapper::isEnabled, Boolean.TRUE::equals); }
        public SELF isDisabled() { return check("to be disabled", null, wrapper::isEnabled, Boolean.FALSE::equals); }

        public SELF hasProperty(String name, Object value) {
            String expected = String.valueOf(value);
            return check("to have property " + name, expected, () -> element.getProperty(name), expected::equals);
        }

        /** Needs {@code @RequiresBrowser}. */
        public SELF hasCssValue(String property, String value) {
            run.requireBrowser("hasCssValue");
            return check("to have CSS " + property, value, () -> element.getCssValue(property),
                    value::equals);
        }
    }

    public static final class GenericAssert extends WrapperAssert<GenericAssert> {
        GenericAssert(UnifiedWrapper wrapper) { super(wrapper); }
    }

    public static final class TextFieldAssert extends WrapperAssert<TextFieldAssert> {
        private final UnifiedTextField field;

        TextFieldAssert(UnifiedTextField field) { super(field); this.field = field; }

        public TextFieldAssert hasValue(String expected) {
            return check("to have value", expected, field::getValue, v -> Objects.equals(v, expected));
        }

        public TextFieldAssert isEmpty() {
            return check("to be empty", null, field::getValue, v -> v == null || v.toString().isEmpty());
        }

        public TextFieldAssert isValid() { return check("to be valid", null, field::isInvalid, Boolean.FALSE::equals); }
        public TextFieldAssert isInvalid() { return check("to be invalid", null, field::isInvalid, Boolean.TRUE::equals); }

        public TextFieldAssert hasErrorMessage(String expected) {
            return check("to have error message", expected, field::getErrorMessage, v -> Objects.equals(v, expected));
        }

        public TextFieldAssert isRequired() { return check("to be required", null, field::isRequired, Boolean.TRUE::equals); }
    }

    public static final class ButtonAssert extends WrapperAssert<ButtonAssert> {
        private final UnifiedButton button;

        ButtonAssert(UnifiedButton button) { super(button); this.button = button; }

        public ButtonAssert hasText(String expected) {
            return check("to have text", expected, button::getText, v -> Objects.equals(v, expected));
        }
    }

    public static final class DialogAssert extends WrapperAssert<DialogAssert> {
        private final UnifiedDialog dialog;

        DialogAssert(UnifiedDialog dialog) { super(dialog); this.dialog = dialog; }

        public DialogAssert isOpen() {
            return check("to be open", null, dialog::isOpen, Boolean.TRUE::equals);
        }

        /** Passes when the dialog is closed or no longer on the page (Flow removes closed dialogs). */
        public DialogAssert isClosed() {
            return check("to be closed", null,
                    () -> element.tryConcrete().map(e -> Boolean.TRUE.equals(e.getPropertyBoolean("opened"))).orElse(false),
                    Boolean.FALSE::equals);
        }

        public DialogAssert hasHeaderTitle(String expected) {
            return check("to have header title", expected, dialog::getHeaderTitle, v -> java.util.Objects.equals(v, expected));
        }
    }

    public static final class GridAssert extends WrapperAssert<GridAssert> {
        private final UnifiedGrid grid;

        GridAssert(UnifiedGrid grid) { super(grid); this.grid = grid; }

        public GridAssert hasRowCount(int expected) {
            return check("to have row count", expected, grid::getRowCount, v -> Objects.equals(v, expected));
        }

        public GridAssert isEmpty() {
            return check("to be empty", null, grid::getRowCount, v -> Objects.equals(v, 0));
        }
    }

    public static final class GridRowAssert {
        private final UnifiedGrid.UnifiedGridRow row;
        private final TestRun run;

        GridRowAssert(UnifiedGrid.UnifiedGridRow row) { this.row = row; this.run = GridSupport.runOf(row.grid()); }

        public GridRowAssert hasCellText(String columnHeader, String expected) {
            UnifiedAssertions.check(run, row + " cell \"" + columnHeader + "\"", "to have text", expected,
                    () -> row.getCellText(columnHeader), v -> Objects.equals(v, expected));
            return this;
        }
    }

    public static final class GridColumnAssert {
        private final UnifiedGrid.UnifiedGridColumn column;
        private final TestRun run;

        GridColumnAssert(UnifiedGrid.UnifiedGridColumn column) {
            this.column = column;
            this.run = GridSupport.runOf(column.grid());
        }

        public GridColumnAssert hasCellTexts(String... expected) {
            List<String> list = List.of(expected);
            UnifiedAssertions.check(run, column.toString(), "to have cell texts", list, column::getCellTexts,
                    v -> Objects.equals(v, list));
            return this;
        }
    }

    public static final class NotificationAssert extends WrapperAssert<NotificationAssert> {
        private final UnifiedNotification notification;

        NotificationAssert(UnifiedNotification notification) { super(notification); this.notification = notification; }

        public NotificationAssert isOpen() {
            return check("to be open", null, notification::isOpen, Boolean.TRUE::equals);
        }

        public NotificationAssert hasText(String expected) {
            return check("to have text", expected, notification::getText, v -> Objects.equals(v, expected));
        }

        public NotificationAssert hasTextContaining(String part) {
            return check("to have text containing", part, notification::getText, v -> v != null && v.toString().contains(part));
        }
    }
}
