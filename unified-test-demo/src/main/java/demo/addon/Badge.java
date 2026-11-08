package demo.addon;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

/** An add-on component whose text is written by client-side JavaScript: there is none on Browserless. */
@Tag("demo-badge")
public class Badge extends Component {

    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
        render();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        render();
    }

    private void render() {
        getElement().executeJs("this.textContent = $0 + ' new'", count);
    }
}
