// PROTOTYPE: just enough of UnifiedPage to drive one view on either Testing Tool.
package demo.unified;

import com.vaadin.flow.component.Component;

public interface UnifiedPage extends AutoCloseable {
    void navigate(Class<? extends Component> view);
    void setTextField(String label, String value);
    void clickButton(String text);
    String textOf(String id);

    TestingTool testingTool();
    String baseUrl();   // null on Browserless

    @Override
    void close();
}
