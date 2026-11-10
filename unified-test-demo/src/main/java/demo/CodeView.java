package demo;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Native inputs, as custom components use them: Flow's {@code Input}, and a bare {@code <input>} element. */
@AnonymousAllowed
@Route("code")
public class CodeView extends VerticalLayout {

    public CodeView() {
        Input code = new Input();
        code.setTestId("code");
        Span codeValue = new Span();
        codeValue.setId("code-value");
        code.addValueChangeListener(e -> codeValue.setText(e.getValue()));

        Element raw = new Element("input");
        Span rawValue = new Span();
        rawValue.setId("raw-value");
        raw.addPropertyChangeListener("value", "change", e -> rawValue.setText(raw.getProperty("value", "")));
        Div rawHolder = new Div();
        rawHolder.setTestId("raw");
        rawHolder.getElement().appendChild(raw);

        add(code, codeValue, rawHolder, rawValue);
    }
}
