package com.vaadin.testing.unified.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.Scope;
import com.vaadin.testing.unified.query.ComponentQuery;

/** A Scope inside a lazy element, e.g. a grid cell: queries search inside it, found again on every use. */
public record LazyScope(LazyElement element) implements Scope {

    @Override
    public <Q> Q find(Class<? extends Findable<Q>> wrapperType) {
        return Wrappers.newQuery(element.run(), wrapperType, element);
    }

    @Override
    public <C extends Component> ComponentQuery findComponent(Class<C> componentType) {
        return Wrappers.newComponentQuery(element.run(), componentType, element);
    }

    @Override
    public String toString() {
        return element.describe();
    }
}
