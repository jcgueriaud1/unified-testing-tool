package com.vaadin.testing.unified.component;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;

/**
 * Untyped container for layouts (not in the Core Set), found by test id or id, used as a Scope.
 * The component class here is a sketch placeholder: a container matches any layout.
 */
@UnifiedComponent(VerticalLayout.class)
public interface UnifiedContainer extends UnifiedWrapper, Scope, Findable<UnifiedContainer.ContainerQuery> {

    interface ContainerQuery extends Query<UnifiedContainer, ContainerQuery> {}
}
