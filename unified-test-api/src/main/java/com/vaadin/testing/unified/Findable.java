package com.vaadin.testing.unified;

/**
 * Implemented by every wrapper type. {@code Q} is the query that {@code find(WrapperType.class)}
 * returns, so filters are checked per type at compile time (ADR 0001, ADR 0002).
 */
public interface Findable<Q> {}
