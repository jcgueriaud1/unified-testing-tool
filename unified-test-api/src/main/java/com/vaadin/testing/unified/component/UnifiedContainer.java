package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;

/**
 * Any component used to narrow a search, typically a layout (FormLayout, VerticalLayout, Div, …), which has
 * no wrapper of its own. Find it by test id or id, then search inside it:
 *
 * <pre>{@code
 * UnifiedContainer billing = page.find(UnifiedContainer.class).withTestId("billing").single();
 * billing.find(UnifiedTextField.class).withLabel("City").single().setValue("Turku");
 * }</pre>
 */
@UnifiedComponent(value = Component.class, tag = "*")
public interface UnifiedContainer extends UnifiedWrapper, Scope, Findable<UnifiedContainer.ContainerQuery> {

    interface ContainerQuery extends Query<UnifiedContainer, ContainerQuery> {}
}
