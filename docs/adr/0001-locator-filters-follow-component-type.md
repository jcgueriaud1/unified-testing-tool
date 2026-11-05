---
status: accepted
---

# Locator filters follow the component type and are checked at compile time

A Unified API query offers only the filters that make sense for its wrapper type, checked at compile time, so `find(Button).withLabel("Save")` does not compile; a button is found with `withText` or `withAccessibleName` instead. We chose this because a filter that cannot match should fail where it is written, not as an empty result at runtime: TestBench lets `withLabel` compile on a Button and silently matches nothing (its text fallback lives only in `withCaption`), while Browserless already rejects it at compile time. `withLabel` means a visible label (the `label` property, a `<label for>`, or the FormLayout item label) on every type that has one, and `withAccessibleName` covers "find it by what it says" for every type, including icon-only buttons.

## Considered Options

- **`withLabel` on Button as an alias for `withText`**: rejected because it gives `withLabel` two meanings depending on the type.
- **A TestBench-style `withCaption` on every type** (label, then placeholder, then text): rejected because it hides which property matched and still allows filters that can never match.

## Consequences

- Migrating TestBench tests needs `$(ButtonElement.class).withLabel(..)` and `withCaption(..)` calls rewritten to `withText` or `withAccessibleName`.
- Each wrapper type declares its filter set, so a custom wrapper for an add-on component must declare which filters it supports.

Decided in "How does a test locate a component?" (jcgueriaud1/unified-testing-tool#7).
