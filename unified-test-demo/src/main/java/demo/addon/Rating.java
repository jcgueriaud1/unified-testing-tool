package demo.addon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;

/** A small add-on component: five stars, clicked to set the rating. Server-side only, no client code. */
@Tag("demo-rating")
public class Rating extends Component implements HasLabel {

    public Rating(String label) {
        setLabel(label);
        for (int i = 1; i <= 5; i++) {
            int stars = i;
            Element star = new Element("span");
            star.addEventListener("click", e -> setValue(stars));
            getElement().appendChild(star);
        }
        setValue(0);
    }

    public int getValue() {
        return getElement().getProperty("value", 0);
    }

    public void setValue(int value) {
        getElement().setProperty("value", value);
        for (int i = 0; i < 5; i++) {
            getElement().getChild(i).setText(i < value ? "★" : "☆");
        }
    }
}
