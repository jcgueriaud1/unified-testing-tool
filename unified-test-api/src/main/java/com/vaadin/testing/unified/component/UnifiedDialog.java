package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;

/** A Scope: {@code dialog.find(UnifiedButton.class)} searches inside the dialog. */
@UnifiedComponent(Dialog.class)
public interface UnifiedDialog extends UnifiedWrapper, Scope, Findable<UnifiedDialog.DialogQuery> {

    interface DialogQuery extends Query<UnifiedDialog, DialogQuery> {
        /** The dialog's header title, exactly. A custom filter built from {@code where(..)}. */
        default DialogQuery withHeaderTitle(String title) {
            return where(e -> title.equals(e.getProperty("headerTitle")));
        }
    }

    default String getHeaderTitle() { return element().getProperty("headerTitle"); }
    default boolean isOpen() { return Boolean.TRUE.equals(element().getPropertyBoolean("opened")); }
    /** Like pressing Escape. */
    default void close() { element().pressKey("Escape"); }
}
