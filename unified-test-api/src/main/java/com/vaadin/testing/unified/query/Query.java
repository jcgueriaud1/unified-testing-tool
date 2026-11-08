package com.vaadin.testing.unified.query;

import java.util.List;

/** A typed query. Terminals return lazy wrappers. */
public interface Query<W, SELF> extends CommonFilters<SELF> {
    /** Exactly one match, checked when the wrapper is first used. */
    W single();

    List<W> all();

    boolean exists();
}
