// PROTOTYPE: the run's Testing Tool. Real design: JUnit configuration parameter vaadin.testing.tool
// (#9). Spring's bootstrapper has no access to JUnit configuration parameters, so the prototype reads
// the system property of the same name (Surefire passes it).
package demo.unified;

public enum TestingTool {
    BROWSERLESS, PLAYWRIGHT;

    public static TestingTool current() {
        return valueOf(System.getProperty("vaadin.testing.tool", "browserless").toUpperCase());
    }

    public boolean needsServer() {
        return this != BROWSERLESS;
    }
}
