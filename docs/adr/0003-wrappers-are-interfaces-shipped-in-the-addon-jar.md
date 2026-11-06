---
status: accepted
---

# Wrappers are annotated interfaces, shipped inside the add-on's own jar

A Unified API wrapper is an interface with default methods written on a portable Unified Element (properties, attributes, text, `click`, `setValueAsUser`, events, child lookup), annotated with the Flow component class it wraps (`@UnifiedComponent(Rating.class)`, tag read from `@Tag`); the Unified API implements it at runtime (`InvocationHandler.invokeDefault`), so an author writes no implementation class and no per-Testing-Tool code unless a `@ToolImplementation` override is needed. Add-ons ship their wrapper **inside their own production jar**, in a separate package, with the Unified API as an `optional` dependency, because the Vaadin Directory cannot publish multi-module add-ons and a separate `-testbench`-style artifact per add-on is impractical; production never loads the wrapper, tests get it for free. Components without any wrapper still work through `page.findComponent(X.class)`, and a community bundle can cover popular add-ons whose authors ship none. The Core Set is built with this same public mechanism, so add-on authors are not second-class.

## Considered Options

- **One wrapper per Testing Tool** (today's situation: Dramafinder `VaadinElement`, TestBench `TestBenchElement`, Browserless `ComponentTester`): three implementations to write and keep in sync.
- **Abstract wrapper classes** implemented by authors on a driver SPI: more code per wrapper than an interface, same portability.
- **A separate `-unified-test` artifact per add-on**: the familiar TestBench convention, but incompatible with single-module Directory add-ons; still possible, not recommended.

## Consequences

- The Unified Element defines what "portable" means; on Browserless a property is the server-side value, equal to the browser's only for properties Flow synchronises.
- Add-on jars gain a compile-time-only reference to the Unified API; the optional dependency must never become a runtime requirement.

Decided in "How does a user add a Unified API wrapper for a custom or add-on component?" (jcgueriaud1/unified-testing-tool#18).
