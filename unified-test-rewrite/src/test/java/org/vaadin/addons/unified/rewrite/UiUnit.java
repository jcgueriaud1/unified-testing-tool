package org.vaadin.addons.unified.rewrite;

import org.openrewrite.java.JavaParser;

/**
 * A parser for TestBench UI Unit 9.x (Vaadin 24) sources. UI Unit's testers are compiled against Vaadin 24 and
 * share their names with Browserless Test 25's, so they can't be attributed on this module's classpath: the parser
 * has source stubs of the UI Unit API the tests use, with 9.6.6's signatures.
 */
final class UiUnit {

    private static final String[] STUBS = {
            """
            package com.vaadin.testbench.unit;
            import com.vaadin.flow.component.Component;
            import com.vaadin.flow.component.HasElement;
            import com.vaadin.flow.component.Key;
            import com.vaadin.flow.component.KeyModifier;
            public abstract class BaseUIUnitTest {
                public <T extends Component> T navigate(Class<T> view) { return null; }
                public void fireShortcut(Key key, KeyModifier... modifiers) {}
                public HasElement getCurrentView() { return null; }
                public <T extends ComponentTester<Y>, Y extends Component> T test(Y component) { return null; }
                public <T extends Component> ComponentQuery<T> $(Class<T> type) { return null; }
                public <T extends Component> ComponentQuery<T> $view(Class<T> type) { return null; }
                protected static void roundTrip() {}
            }
            """,
            """
            package com.vaadin.testbench.unit;
            public abstract class UIUnitTest extends BaseUIUnitTest implements TesterWrappers {}
            """,
            """
            package com.vaadin.testbench.unit;
            public abstract class UIUnit4Test extends BaseUIUnitTest implements TesterWrappers {}
            """,
            """
            package com.vaadin.testbench.unit;
            public abstract class SpringUIUnitTest extends UIUnitTest {}
            """,
            """
            package com.vaadin.testbench.unit;
            import com.vaadin.flow.component.Component;
            public class ComponentTester<T extends Component> {
                public boolean isUsable() { return true; }
                public T getComponent() { return null; }
            }
            """,
            """
            package com.vaadin.testbench.unit;
            import com.vaadin.flow.component.Component;
            public class ComponentQuery<T extends Component> {
                public ComponentQuery<T> withCaption(String caption) { return this; }
                public ComponentQuery<T> withText(String text) { return this; }
                public ComponentQuery<T> withId(String id) { return this; }
                public T single() { return null; }
                public T first() { return null; }
                public T last() { return null; }
                public T atIndex(int index) { return null; }
            }
            """,
            """
            package com.vaadin.testbench.unit;
            import com.vaadin.flow.component.button.Button;
            import com.vaadin.flow.component.button.ButtonTester;
            import com.vaadin.flow.component.html.Div;
            import com.vaadin.flow.component.html.testbench.DivTester;
            import com.vaadin.flow.component.notification.Notification;
            import com.vaadin.flow.component.notification.NotificationTester;
            import com.vaadin.flow.component.textfield.TextField;
            import com.vaadin.flow.component.textfield.TextFieldTester;
            public interface TesterWrappers {
                default ButtonTester<Button> test(Button button) { return null; }
                default NotificationTester<Notification> test(Notification notification) { return null; }
                default TextFieldTester<TextField, String> test(TextField field) { return null; }
                default DivTester test(Div div) { return null; }
            }
            """,
            """
            package com.vaadin.flow.component.button;
            public class ButtonTester<T extends Button> extends com.vaadin.testbench.unit.ComponentTester<T> {
                public void click() {}
            }
            """,
            """
            package com.vaadin.flow.component.notification;
            public class NotificationTester<T extends Notification> extends com.vaadin.testbench.unit.ComponentTester<T> {
                public String getText() { return null; }
            }
            """,
            """
            package com.vaadin.flow.component.textfield;
            public class TextFieldTester<T extends TextField, V> extends com.vaadin.testbench.unit.ComponentTester<T> {
                public void setValue(V value) {}
                public void clear() {}
            }
            """,
            """
            package com.vaadin.flow.component.html.testbench;
            public class DivTester extends com.vaadin.testbench.unit.ComponentTester<com.vaadin.flow.component.html.Div> {
                public void click() {}
            }
            """
    };

    private UiUnit() {
    }

    static JavaParser.Builder<?, ?> parser() {
        // only what the tests' sources use: the whole test classpath and source stubs together break javac
        return JavaParser.fromJavaVersion()
                .classpath("flow-server", "flow-html-components", "vaadin-button-flow", "vaadin-text-field-flow",
                        "vaadin-notification-flow", "vaadin-dialog-flow", "vaadin-flow-components-base", "flow-data",
                        "junit-jupiter-api", "junit",
                        "spring-boot-test", "spring-test", "spring-context")
                .dependsOn(STUBS);
    }
}
