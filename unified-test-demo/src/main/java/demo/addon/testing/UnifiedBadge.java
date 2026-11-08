package demo.addon.testing;

import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.ToolImplementation;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import demo.addon.Badge;

/**
 * Reads the count from the text the browser shows. Browserless runs no client-side JavaScript, so the badge
 * has no text there: {@link UnifiedBadgeBrowserless} reads the Java API instead.
 */
@UnifiedComponent(value = Badge.class,
        overrides = @ToolImplementation(tool = TestingTool.BROWSERLESS, value = UnifiedBadgeBrowserless.class))
public interface UnifiedBadge extends UnifiedWrapper, Findable<UnifiedBadge.BadgeQuery> {

    interface BadgeQuery extends Query<UnifiedBadge, BadgeQuery> {}

    /** From the text "3 new". */
    default int getCount() {
        String text = element().getText().strip();
        return text.isEmpty() ? 0 : Integer.parseInt(text.split(" ")[0]);
    }
}
