package demo;

import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.query.TextFilter;

/** The selector tells the cards apart from the other vertical layouts, on every Testing Tool. */
// --8<-- [start:wrapper]
@UnifiedComponent(value = MetricCard.class, selector = ".metric-card")
public interface UnifiedMetricCard extends UnifiedWrapper, Findable<UnifiedMetricCard.MetricCardQuery> {

    interface MetricCardQuery extends Query<UnifiedMetricCard, MetricCardQuery>, TextFilter<MetricCardQuery> {}

    default String getTitle() { return element().findChildren("span").get(0).getText(); }
    default String getValue() { return element().findChildren("span").get(1).getText(); }
}
// --8<-- [end:wrapper]
