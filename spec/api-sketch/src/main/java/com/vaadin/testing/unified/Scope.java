package com.vaadin.testing.unified;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.query.ComponentQuery;

/** Something a test can search in: the page, a dialog, a container. */
public interface Scope {

    /** Typed lookup: {@code find(UnifiedTextField.class).withLabel("Name").single()}. */
    <Q> Q find(Class<? extends Findable<Q>> wrapperType);

    /** Lookup of any Flow component without a wrapper; the tag comes from its {@code @Tag}. */
    <C extends Component> ComponentQuery findComponent(Class<C> componentType);
}
