---
status: accepted
---

# Wrappers are named `Unified*` and found by class literal

Unified API wrappers carry a `Unified` prefix (`UnifiedTextField`, `UnifiedButton`, `UnifiedDialog`) and are found with their class literal: `ui.find(UnifiedTextField.class).withLabel("Name").single()`. Every plain or suffixed name is already taken by something a test may import at the same time: `TextField` (Flow, and what Browserless's `unwrap(Component.class)` returns), `TextFieldElement` (both Dramafinder and TestBench), `TextFieldTester` and `TextFieldLocator` (Browserless); an Escape Hatch (`unwrap`) can bring several of these into one test file, so the prefix guarantees no import clash. Each wrapper type declares its query type (`UnifiedTextField extends Findable<TextFieldQuery>`), and `<Q> Q find(Class<? extends Findable<Q>> type)` lets Java infer the per-type query from the class literal, keeping filters checked at compile time (ADR 0001) while staying open to custom components, whose wrappers declare `Findable<MyQuery>`.

## Considered Options

- **Generated finders** (`ui.findTextField()`, Browserless style): discoverable, but the entry object must know every type, so custom and add-on components can't join.
- **Specs as values** (`ui.find(TextField.withLabel("Name"))`): reusable, but static factories live on the wrapper type, which then collides with Flow's class name.
- **Type-token constants** (`ui.find(TEXT_FIELD)`): open and per-type, but needs static imports; unnecessary once class literals were shown to infer the query type.
- **`*Element` suffix**: familiar to TestBench and Dramafinder users, but those two libraries already both use it.

Decided in "What does a component wrapper look like for Button and TextField?" (jcgueriaud1/unified-testing-tool#12); prototype and comparison on branch `prototype/component-wrapper`.
