package com.vaadin.testing.unified;

/** The tool a run executes on. Chosen per run with the JUnit configuration parameter {@code vaadin.testing.tool}. */
public enum TestingTool {
    /** Vaadin Browserless Test: in the test JVM, no browser. The default. */
    BROWSERLESS,
    /** Dramafinder on Playwright: real browser. */
    PLAYWRIGHT,
    /** TestBench on Selenium: real browser. */
    SELENIUM
}
