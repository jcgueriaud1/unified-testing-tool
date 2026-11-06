# PROTOTYPE: component wrapper API (throwaway)

Answers "What does a component wrapper look like for Button and TextField?" (#12) on the map
"Unified testing API for Vaadin" (#2). Never merged; lives on branch `prototype/component-wrapper`.

Type stubs only, no behaviour: it checks how tests **read** and that the decided rules
(per-type filters, ADR 0001) **compile**. Same scenario, `ProfileViewTest`, in three lookup styles:

| Variant | Lookup | Trade-off |
|---|---|---|
| `v1` generated finders | `ui.findTextField().withLabel("Name").single()` | discoverable; closed to custom components |
| `v2` specs as values | `ui.find(TextField.withLabel("Name"))` | reusable spec constants, reads well; wrapper names collide with Flow classes |
| `v3` class literal (**chosen**) | `ui.find(UnifiedTextField.class).withLabel("Name").single()` | open to custom components; no static imports |

Shared by all variants (`src/proto/common`): wrappers, retrying `assertThat(...)`, filter mixins, Escape Hatch.

Run (JDK 21):

```
javac -d /tmp/proto-out $(find src -name '*.java')
```

Uncomment the `withLabel` line on a Button in any `ProfileViewTest` to see it fail to compile.

## Verdict

V3 with class literals: `ui.find(UnifiedTextField.class)`, where each wrapper declares `Findable<Q>` so Java infers its per-type query. Wrappers use the `Unified` prefix (TextField, TextFieldElement, TextFieldTester and TextFieldLocator are all taken by Flow and the Testing Tools). Static `assertThat(...)` retrying assertions. Queries end with `single()`, `all()` or `exists()`.
