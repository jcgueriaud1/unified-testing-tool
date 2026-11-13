# Finding components

Every lookup is a **query** with three parts:

```java
page.find(UnifiedTextField.class)   // 1. start: which kind of component
    .withLabel("Name")              // 2. filters: zero or more, all must match
    .single();                      // 3. terminal: single(), all() or exists()
```

Spec: [4. Locating components](../spec/04-locating.md).

## 1. Start: the wrapper class

The wrapper class is the component type, like a role in Playwright's `getByRole`. Each wrapper has its own query type, which offers only the filters that make sense for it:

```java
page.find(UnifiedButton.class).withText("Save");      // a button has text
page.find(UnifiedTextField.class).withLabel("Name");  // a field has a label
page.find(UnifiedButton.class).withLabel("Save");     // does not compile: a button has no label
```

So your IDE's completion after `find(UnifiedX.class).` lists exactly what you can filter by.

Wrappers live in `com.vaadin.testing.unified.component`: `UnifiedTextField`, `UnifiedTextArea`, `UnifiedNumberField`, `UnifiedIntegerField`, `UnifiedBigDecimalField`, `UnifiedDatePicker`, `UnifiedDateTimePicker`, `UnifiedCheckbox`, `UnifiedComboBox`, `UnifiedMultiSelectComboBox`, `UnifiedRadioButtonGroup`, `UnifiedButton`, `UnifiedMenuBar`, `UnifiedDialog`, `UnifiedNotification`, `UnifiedDetails`, `UnifiedUpload`, `UnifiedGrid`, `UnifiedTabSheet`, `UnifiedTabs`, `UnifiedContainer` and the plain HTML wrappers (`UnifiedDiv`, `UnifiedSpan`, `UnifiedSection`, …). For any other component, see [Components without a wrapper](#components-without-a-wrapper).

## 2. Filters

### By what the user sees (prefer these)

| Filter | Finds | Example |
|---|---|---|
| `withLabel("Email")` | a field by its label | `page.find(UnifiedTextField.class).withLabel("Email")` |
| `withText("Save")` | a button, notification, … by its text | `page.find(UnifiedButton.class).withText("Save")` |
| `withAccessibleName("Delete account")` | what a screen reader announces: `aria-label`, `setAriaLabel(..)`, label or text | an icon-only button, a field without a visible label |
| `withPlaceholder("Nickname")` | a field by its placeholder | `page.find(UnifiedTextField.class).withPlaceholder("Nickname")` |
| `withHeaderTitle("Delete?")` | a dialog by its header | `page.find(UnifiedDialog.class).withHeaderTitle("Change email")` |
| `withTab("Security")` | a TabSheet or Tabs that has this tab | `page.find(UnifiedTabSheet.class).withTab("Security")` |
| `withItem("File")` | a MenuBar that has this top-level item | `page.find(UnifiedMenuBar.class).withItem("File")` |
| `withSummaryText("Advanced")` | a Details by its summary | `page.find(UnifiedDetails.class).withSummaryText("Advanced")` |

An icon-only button has no text, but it has an accessible name:

```java
--8<-- "unified-test-demo/src/test/java/demo/ProfileViewTest.java:findsIconButtonByAccessibleName"
```

A field named with `setAriaLabel(..)` and no visible label is found the same way:

```java
--8<-- "unified-test-demo/src/test/java/demo/ReportTest.java:findsAFieldByItsAccessibleName"
```

### Exact or containing

Text filters match the **whole text**, with whitespace normalised. Each has a `…Containing` variant that matches a part, ignoring case:

```java
page.find(UnifiedButton.class).withText("Save");                      // "Save", not "Save draft"
page.find(UnifiedButton.class).withTextContaining("save");            // "Save", "Save draft", "Autosave"
page.find(UnifiedTextField.class).withLabelContaining("phone");       // "Phone", "Mobile phone"
page.find(UnifiedButton.class).withAccessibleNameContaining("Edit");  // "Edit User 3", "Edit User 4", …
```

Prefer the exact form: a test that matches more than you meant breaks on `single()` the day someone adds a "Save draft" button.

### By technical attributes (fallback)

When a component has nothing a user can read, give it a test id in the view (`component.setTestId("nickname")`) and find it by that:

| Filter | Matches |
|---|---|
| `withTestId("nickname")` | `data-testid`, set with `Component.setTestId` |
| `withId("receipt")` | `id`, set with `Component.setId` |
| `withClassName("summary")` | a CSS class name |
| `withTheme("primary")` | a theme variant |
| `withAttribute("name", "value")` | any attribute |
| `withProperty("label", "Service")` | any property |

`withClassName`, `withTheme` and `withAttribute` depend on how the view is styled, not on what it does. Use them for plain HTML (cards, layouts), not for fields and buttons:

```java
--8<-- "unified-test-demo/src/test/java/demo/TripTest.java:filtersByClassName"
```

### Combining filters

Filters add up: a component must match all of them.

```java
page.find(UnifiedButton.class).withText("Delete").withTheme("error").single();
page.findComponent(Div.class).withClassName("trip-card").withText("Trip 7").all();
```

### Only visible components

A query includes hidden components (`setVisible(false)`), as a browser's page does. Add `.visible()` when visibility is the point, for example "the button is gone after the click":

```java
--8<-- "unified-test-demo/src/test/java/demo/ApprovalTest.java:aHiddenButtonIsStillFoundButNotVisible"
```

## 3. Terminals

| Terminal | Returns | Fails |
|---|---|---|
| `single()` | one lazy wrapper | when it is first used, if zero or several components match |
| `all()` | `List` of lazy wrappers, in page order | never; the list may be empty |
| `exists()` | `boolean`, at once, no waiting | never |

```java
UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
List<UnifiedMetricCard> cards = page.find(UnifiedMetricCard.class).all();
boolean canArchive = page.find(UnifiedButton.class).withText("Archive").visible().exists();
```

There is no `first()`: when several components match, either the filter is not precise enough (add a filter or a [scope](#scopes)), or the order is the point, and `all().get(i)` says so.

### Wrappers are lazy

`single()` does not search yet. The wrapper searches every time you use it, and waits until the component is there. So:

- you can find a component **before** it appears, and use the wrapper once it does;
- a wrapper **never goes stale**: after the view re-renders, it finds the new component;
- keep the wrapper in a variable and reuse it for the whole test.

```java
UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();  // no search yet
name.setValue("Alice");                  // searches, waits until it is there and enabled, types
assertThat(name).hasValue("Alice");      // searches again
```

A missing or ambiguous component is reported where it is used, with the query in the message.

## Scopes

A page has many "City" fields, "Save" buttons and "Email" fields. Narrow the search to a part of the page by starting the query from a **Scope** instead of from the page:

| Scope | Reach it with | Contains |
|---|---|---|
| `UnifiedPage` | the test parameter | the whole page |
| `UnifiedDialog` | `page.find(UnifiedDialog.class)…single()` | the dialog's content |
| `UnifiedContainer` | `page.find(UnifiedContainer.class).withTestId("billing").single()` | any layout or component, by test id or id |
| plain HTML wrappers | `page.find(UnifiedSection.class)…`, `UnifiedDiv`, … | that element |
| `findComponent(..)` result | `page.findComponent(VerticalLayout.class)…single()` | that component |
| `UnifiedDetails.content()` | `details.content()` | the Details content |
| `UnifiedTabSheet.content()` | `tabs.content()` | the selected tab's content |
| Grid cells, header, row details | `row.cell("Actions")`, `column.header()`, `row.details()` | see [Grid](grid.md) |

### A dialog

```java
--8<-- "unified-test-demo/src/test/java/demo/DialogTest.java:helpers"

--8<-- "unified-test-demo/src/test/java/demo/DialogTest.java:searchesInsideTheDialog"
```

### A form section

Two forms with the same labels: give each layout a test id or id in the view, and search inside it.

```java
--8<-- "unified-test-demo/src/test/java/demo/CheckoutTest.java:picksTheFieldThroughItsContainer"
```

Scopes nest:

```java
--8<-- "unified-test-demo/src/test/java/demo/CheckoutTest.java:containersNest"
```

### Details and TabSheet content

```java
--8<-- "unified-test-demo/src/test/java/demo/ExpenseTest.java:opensDetails"
```

A TabSheet's `content()` follows the selected tab: the same Scope searches the new tab after `selectTab(..)`.

```java
--8<-- "unified-test-demo/src/test/java/demo/AccountTest.java:contentFollowsTheSelectedTab"
```

### Plain HTML

```java
--8<-- "unified-test-demo/src/test/java/demo/ExpenseTest.java:searchesInPlainHtmlContainers"
```

## Components without a wrapper

`findComponent(FlowClass.class)` finds any Flow component with a `@Tag`, without writing a wrapper. It has the shared filters (label, text, accessible name, test id, …) and gives a generic wrapper with the [Unified Element](../spec/06-wrappers.md#unified-element) underneath:

```java
--8<-- "unified-test-demo/src/test/java/demo/AddonTest.java:anyComponentWithoutAWrapper"
```

It is handy for reading text from a `Span`, `H2` or `Paragraph`:

```java
assertThat(page.findComponent(Span.class).withId("status").single()).hasText("Approved");
assertThat(page.findComponent(H2.class).withText("Admin area of alice").single()).isVisible();
```

A component without a tag (a `Composite`, `Component.class`) can't be found by a browser, so `findComponent` refuses it at once on every Testing Tool. Find its root component instead, or write a [wrapper](custom-wrappers.md) with `tag = "…"`.

## Custom filters

When a filter you need doesn't exist, build it from `withProperty(name, value)` or `where(Predicate<UnifiedElement>)`:

```java
--8<-- "unified-test-demo/src/test/java/demo/AddonTest.java:customFilters"
```

`withValue(4)` is a default method on the wrapper's query, written once with `where(..)`; see [Wrappers for your own components](custom-wrappers.md#custom-filters).

## Cheat sheet

| I want… | Write |
|---|---|
| the field labelled *Name* | `page.find(UnifiedTextField.class).withLabel("Name").single()` |
| the *Save* button | `page.find(UnifiedButton.class).withText("Save").single()` |
| an icon button | `page.find(UnifiedButton.class).withAccessibleName("Delete account").single()` |
| the *Save* button of the dialog | `dialog.find(UnifiedButton.class).withText("Save").single()` |
| the *City* field of the billing form | `page.find(UnifiedContainer.class).withTestId("billing").single().find(UnifiedTextField.class).withLabel("City").single()` |
| the open dialog | `page.find(UnifiedDialog.class).single()` |
| the notification saying *Saved* | `page.find(UnifiedNotification.class).withText("Saved").single()` |
| how many cards there are | `page.find(UnifiedMetricCard.class).all().size()` |
| whether a button is shown right now | `page.find(UnifiedButton.class).withText("Archive").visible().exists()` |
| text in a `Span` | `page.findComponent(Span.class).withId("status").single().getText()` |
| a Grid row | `grid.rows().withCellText("Name", "Alice").single()` ([Grid](grid.md)) |
