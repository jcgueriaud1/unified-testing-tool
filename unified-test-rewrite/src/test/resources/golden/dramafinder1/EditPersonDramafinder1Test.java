package demo;

import org.junit.jupiter.api.Test;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

/** Dramafinder 1.x, without the Unified API: the golden source of the 1.x migration. */
class EditPersonDramafinder1Test extends SpringPlaywrightIT {

    @Override
    public String getView() {
        return "people";
    }

    @Test
    void editsAPersonFromTheGrid() {
        GridElement people = GridElement.get(page);

        GridElement.RowElement alan = people.findRow(people.findRowIndexesWithColumnText(0, "Alan Turing").get(0))
                .orElseThrow();
        alan.getCell("Name").click();
        TextFieldElement.getByLabel(page, "Name").setValue("Alan M. Turing");
        ButtonElement.getByText(page, "Save").click();

        NotificationElement.getByText(page, "Saved Alan M. Turing").assertOpen();
        alan.getCell("Name").assertText("Alan M. Turing");
    }
}
