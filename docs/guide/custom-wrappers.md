# Wrappers for your own components

`page.findComponent(MyComponent.class)` already works for any component with a `@Tag` ([Components without a wrapper](finding-components.md#components-without-a-wrapper)). Write a wrapper when tests use a component often: it gives the component its own `find(..)`, its own filters and actions named after what a user does. Spec: [6. Wrappers](../spec/06-wrappers.md#custom-and-add-on-components).

## A wrapper is an interface

No implementation class: the Unified API implements the interface at run time, on every Testing Tool. Methods are default methods written on `element()`, the [Unified Element](../spec/06-wrappers.md#unified-element).

```java
--8<-- "unified-test-demo/src/main/java/demo/addon/testing/UnifiedRating.java:wrapper"
```

| Part | Role |
|---|---|
| `@UnifiedComponent(Rating.class)` | the Flow class for Browserless; the tag for browsers comes from its `@Tag` (`tag = "…"` for a `Composite`) |
| `extends UnifiedWrapper, Findable<RatingQuery>` | makes it findable with `page.find(UnifiedRating.class)` |
| `RatingQuery` | the filters it accepts: `LabelFilter`, `TextFilter`, `PlaceholderFilter`, plus your own |
| default methods | actions and getters, written on `element()` |

Use it like a built-in wrapper:

```java
UnifiedRating food = page.find(UnifiedRating.class).withLabel("Food").single();
food.rate(3);
page.waitUntil(() -> food.getValue() == 3);
```

Wrapper interfaces must be public, and so must any class they are nested in. An add-on can ship its wrappers in its own jar, with the Unified API as an `optional` dependency ([ADR 0003](../adr/0003-wrappers-are-interfaces-shipped-in-the-addon-jar.md)).

## Custom filters

A filter is a default method on the query, built from `withProperty(..)`, `withAttribute(..)` or `where(Predicate<UnifiedElement>)`. `withValue(int)` above is one:

```java
assertEquals(1, page.find(UnifiedRating.class).withValue(4).all().size());
```

## A component built from a common one

`MetricCard extends VerticalLayout` has the tag of a `VerticalLayout`: a browser can't tell it apart by class. Add a `selector` with what does:

```java
--8<-- "unified-test-demo/src/test/java/demo/UnifiedMetricCard.java:wrapper"
```

```java
assertEquals("12", page.find(UnifiedMetricCard.class).withTextContaining("Trips").single().getValue());
```

`selector` is a compound CSS selector of `.class`, `#id`, `[attribute="value"]` and `[theme~="value"]`, without spaces.

## When one Testing Tool needs other code

Browserless runs no client-side JavaScript, so content the browser draws isn't there. Override the methods that need it for that tool, in an interface extending the wrapper:

```java
@UnifiedComponent(value = Badge.class,
        overrides = @ToolImplementation(tool = TestingTool.BROWSERLESS, value = UnifiedBadgeBrowserless.class))
public interface UnifiedBadge extends UnifiedWrapper, Findable<UnifiedBadge.BadgeQuery> {

    interface BadgeQuery extends Query<UnifiedBadge, BadgeQuery> {}

    /** From the text "3 new" the browser shows. */
    default int getCount() {
        String text = element().getText().strip();
        return text.isEmpty() ? 0 : Integer.parseInt(text.split(" ")[0]);
    }
}

public interface UnifiedBadgeBrowserless extends UnifiedBadge {
    @Override
    default int getCount() { return unwrap(Badge.class).getCount(); }   // the Java component
}
```

Tests use `UnifiedBadge` and stay the same on every tool.
