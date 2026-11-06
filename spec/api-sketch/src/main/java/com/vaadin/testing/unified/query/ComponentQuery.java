package com.vaadin.testing.unified.query;

import com.vaadin.testing.unified.component.UnifiedGenericComponent;

/** Query for a Flow component without a wrapper: shared filters only. */
public interface ComponentQuery extends Query<UnifiedGenericComponent, ComponentQuery>,
        LabelFilter<ComponentQuery>, TextFilter<ComponentQuery> {}
