package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Browserless Test, without the Unified API. Shown in docs/comparison.md. */
// --8<-- [start:test]
@SpringBootTest
class EditPersonBrowserlessTest extends SpringBrowserlessTest {

    @Test
    @SuppressWarnings("unchecked")
    void editsAPersonFromTheGrid() {
        navigate(PeopleView.class);
        GridTester<Grid<PeopleView.Person>, PeopleView.Person> people = test($(Grid.class).single());

        int row = IntStream.range(0, people.size())
                .filter(i -> people.getCellText(i, 0).equals("Alan Turing"))
                .findFirst().orElseThrow();
        people.clickRow(row);
        test($(TextField.class).withLabel("Name").single()).setValue("Alan M. Turing");
        test($(Button.class).withText("Save").single()).click();

        assertEquals("Saved Alan M. Turing", test($(Notification.class).single()).getText());
        assertEquals("Alan M. Turing", people.getCellText(row, 0));
    }
}
// --8<-- [end:test]
