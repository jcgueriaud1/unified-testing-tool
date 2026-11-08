package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.query.TextFilter;

/** No label filter: a button is found by text or accessible name (ADR 0001). */
@UnifiedComponent(Button.class)
public interface UnifiedButton extends UnifiedWrapper, Findable<UnifiedButton.ButtonQuery> {

    interface ButtonQuery extends Query<UnifiedButton, ButtonQuery>, TextFilter<ButtonQuery> {}

    default void click() { element().click(); }
    default String getText() { return element().getText(); }
}
