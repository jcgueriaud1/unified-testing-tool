package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedPage;
import com.vaadin.testing.unified.component.UnifiedContainer;
import com.vaadin.testing.unified.spring.UnifiedSpringBootTest;
import java.util.List;
import org.junit.jupiter.api.Test;

/** findChildren, children and findDescendants: light DOM only, the same on every Testing Tool. */
@UnifiedSpringBootTest
class TreeTest {

    private static UnifiedElement tree(UnifiedPage page) {
        page.navigate(TreeView.class);
        return page.find(UnifiedContainer.class).withTestId("tree").single().element();
    }

    private static List<String> tags(List<UnifiedElement> elements) {
        return elements.stream().map(UnifiedElement::getTag).toList();
    }

    @Test
    void listsDirectChildrenInOrder(UnifiedPage page) {
        UnifiedElement tree = tree(page);

        assertEquals(List.of("span", "div", "vaadin-button", "vaadin-form-layout"), tags(tree.children()));
        assertEquals(List.of("one"), tree.findChildren("span").stream().map(UnifiedElement::getText).toList());
        assertEquals(1, tree.findChildren("vaadin-form-layout").get(0).children().size());
    }

    @Test
    void findsDescendantsAtEveryLevel(UnifiedPage page) {
        UnifiedElement tree = tree(page);

        assertEquals(List.of("one", "nested"),
                tree.findDescendants("span").stream().map(UnifiedElement::getText).toList());
        assertEquals(1, tree.findDescendants("vaadin-text-field").size());
        assertEquals(1, tree.findChildren("vaadin-button").get(0).findChildren("vaadin-icon").size());
    }
}
