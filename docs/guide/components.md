# Working with components

Once [found](finding-components.md), a wrapper acts like a user: it waits until the component is visible and enabled, acts, then waits until the server has answered (Auto-Wait). Spec: [6. Wrappers](../spec/06-wrappers.md).

Every wrapper has:

| Method | Does |
|---|---|
| `isVisible()`, `isEnabled()` | reads the state now |
| `withTimeout(Duration)` | the same wrapper with its own timeout |
| `element()` | the [Unified Element](../spec/06-wrappers.md#unified-element) underneath: text, properties, attributes, `click()`, `pressKey(..)` |
| `unwrap(..)`, `tryUnwrap(..)` | the Testing Tool's own object ([Escape Hatch](tool-specific.md)) |

## Text fields

```java
UnifiedTextField name = page.find(UnifiedTextField.class).withLabel("Name").single();
name.setValue("Alice");     // types as a user: fires input and change
name.clear();
String value = name.getValue();

assertThat(name).hasValue("Alice").isValid().hasLabel("Name");
assertThat(page.find(UnifiedTextField.class).withLabel("Email").single())
        .isInvalid()
        .hasErrorMessage("Email is required");
```

`UnifiedTextArea` works the same way.

## Typed values

Number and date fields take and return Java types, `null` when empty:

| Wrapper | Value type |
|---|---|
| `UnifiedNumberField` | `Double` |
| `UnifiedIntegerField` | `Integer` |
| `UnifiedBigDecimalField` | `BigDecimal` |
| `UnifiedDatePicker` | `LocalDate` |
| `UnifiedDateTimePicker` | `LocalDateTime` |

```java
--8<-- "unified-test-demo/src/test/java/demo/ExpenseTest.java:fillsTheValueFields"
```

To check how the field handles text it can't parse, type it:

```java
UnifiedDatePicker returnDate = page.find(UnifiedDatePicker.class).withLabel("Return date").single();
returnDate.typeText("dsdds");
assertThat(returnDate).isInvalid();

departure.typeText("dsdds", "10:00");   // UnifiedDateTimePicker: date text, time text
```

## Checkbox

```java
--8<-- "unified-test-demo/src/test/java/demo/PreferencesTest.java:savesTypedValues"
```

`check()` and `uncheck()` do nothing when the checkbox is already in that state; `setChecked(boolean)` does either. Assertions: `isChecked()`, `isNotChecked()`, `isIndeterminate()`.

## Selection: ComboBox, RadioButtonGroup, MultiSelectComboBox

Items are named by the **label the user sees** (the item label generator's result), not by the Java object:

```java
--8<-- "unified-test-demo/src/test/java/demo/OrderTest.java:selectsByVisibleLabel"
```

| | `UnifiedComboBox`, `UnifiedRadioButtonGroup` | `UnifiedMultiSelectComboBox` |
|---|---|---|
| select | `selectItem("Germany")` | `selectItems("Olives", "Cheese")` |
| deselect | `clear()` | `deselectItems("Olives")`, `clear()` |
| read | `getSelectedItem()`, `getItems()` | `getSelectedItems()`, `getItems()` |
| assert | `hasSelectedItem(..)`, `hasNoSelectedItem()`, `hasItems(..)` | `hasSelectedItems(..)`, `hasNoSelectedItems()`, `hasItems(..)` |

Selecting an item that doesn't exist fails with the list of items there are, e.g. `… [Finland, Germany, France]`.

## Buttons and menus

```java
page.find(UnifiedButton.class).withText("Save").single().click();
assertThat(page.find(UnifiedButton.class).withText("Archive").single()).isDisabled();
```

A MenuBar opens its sub-menus as a user does: give the path of item names.

```java
--8<-- "unified-test-demo/src/test/java/demo/ExpenseTest.java:clicksNestedMenuItems"
```

An icon-only item is named by its `aria-label`. `isItemEnabled("Actions for Hotel", "Delete")` reads whether an item can be clicked.

## Tabs and Details

```java
UnifiedTabSheet tabs = page.find(UnifiedTabSheet.class).single();
tabs.selectTab("Security");
assertThat(tabs).hasTabs("Profile", "Security", "Billing").hasSelectedTab("Security");
tabs.content().find(UnifiedButton.class).withText("Change password").single().click();

UnifiedTabs filter = page.find(UnifiedTabs.class).withTab("Approved").single();   // Tabs without content
filter.selectTab("Rejected");

UnifiedDetails advanced = page.find(UnifiedDetails.class).withSummaryText("Advanced").single();
advanced.open();
advanced.content().find(UnifiedTextField.class).withLabel("Cost center").single().setValue("CC-7");
```

A tab holding a `RouterLink` is named by the link's text.

## Dialogs and notifications

Only open dialogs and notifications are found. A dialog is a [Scope](finding-components.md#scopes):

```java
page.find(UnifiedButton.class).withText("Change email").single().click();
UnifiedDialog dialog = page.find(UnifiedDialog.class).withHeaderTitle("Change email").single();
dialog.find(UnifiedTextField.class).withLabel("Email").single().setValue("new@example.com");
dialog.find(UnifiedButton.class).withText("Save").single().click();
assertThat(dialog).isClosed();

dialog.close();                  // as pressing Escape

assertThat(page.find(UnifiedNotification.class).single()).isOpen().hasText("Saved Alice");
assertThat(page.find(UnifiedNotification.class).withText("Account deleted").single()).isOpen();
```

When several notifications can be open at once, filter by text rather than relying on `single()`.

## Upload

`upload` returns once the server has received the file:

```java
--8<-- "unified-test-demo/src/test/java/demo/ExpenseTest.java:uploadsAFile"
```

## Keyboard and DOM events

```java
search.element().pressKey("Enter");                          // to a component
page.find(UnifiedTextField.class).withLabel("Search").single().element().pressKey("Control+s");
page.pressKey("Escape");                                     // to the focused component or the open dialog
dropZone.element().dispatchEvent("item-dropped");            // a custom DOM event, for a Flow listener
```

```java
--8<-- "unified-test-demo/src/test/java/demo/KeyboardTest.java:escapeOnThePageClosesTheDialog"
```

On Browserless, key presses fire the server-side listeners and shortcuts; client-side behaviour such as Tab moving focus doesn't happen.

## Plain HTML and text

```java
assertThat(page.findComponent(Span.class).withId("status").single()).hasText("Approved");
String total = page.find(UnifiedDiv.class).withClassName("summary").single().getText();
page.findComponent(Input.class).withTestId("code").single().element().setValueAsUser("ABC");
```

`getText()` is the DOM text, the same on every Testing Tool: CSS such as `text-transform` doesn't change it. In a browser, `element().getVisibleText()` gives the text as rendered.
