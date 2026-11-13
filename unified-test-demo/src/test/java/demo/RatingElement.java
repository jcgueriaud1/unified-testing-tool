package demo;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/** A project's own TestBench element for the demo's Rating add-on: a custom element the migration leaves behind. */
@Element("demo-rating")
public class RatingElement extends TestBenchElement {

    public void rate(int stars) {
        $("span").all().get(stars - 1).click();
    }

    public int getValue() {
        return getPropertyInteger("value");
    }
}
