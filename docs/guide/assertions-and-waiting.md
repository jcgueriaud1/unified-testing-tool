# Assertions and waiting

In a browser, the page updates a moment after the server answers. A test that reads a value too early fails now and then. The Unified API removes the waits from your test: actions wait, wrappers wait, and assertions retry. Spec: [5. Waiting](../spec/05-waiting.md).

## Assert with `assertThat`

```java
import static com.vaadin.testing.unified.assertion.UnifiedAssertions.assertThat;

assertThat(name).hasValue("Alice").isValid();                         // retries until it holds
assertThat(page.find(UnifiedNotification.class).single()).hasText("Saved Alice");
assertThat(page).isAt(ProfileView.class);
```

Each assertion retries until it holds or the timeout (10 s by default) passes, then fails with the expected and the last actual value. Chained assertions each retry.

| Wrapper | Assertions (besides the common ones) |
|---|---|
| every wrapper | `isVisible()`, `isHidden()`, `isEnabled()`, `isDisabled()`, `hasText(..)`, `hasTextContaining(..)`, `hasProperty(name, value)`, `hasCssValue(..)` (browser only) |
| fields | `hasValue(..)`, `isEmpty()`, `isValid()`, `isInvalid()`, `hasErrorMessage(..)`, `isRequired()`, `hasLabel(..)` |
| `UnifiedCheckbox` | `isChecked()`, `isNotChecked()`, `isIndeterminate()`, `hasLabel(..)` |
| `UnifiedComboBox`, `UnifiedRadioButtonGroup` | `hasSelectedItem(..)`, `hasNoSelectedItem()`, `hasItems(..)` |
| `UnifiedMultiSelectComboBox` | `hasSelectedItems(..)`, `hasNoSelectedItems()`, `hasItems(..)` |
| `UnifiedDialog` | `isOpen()`, `isClosed()`, `hasHeaderTitle(..)` |
| `UnifiedNotification` | `isOpen()`, `hasText(..)` |
| `UnifiedTabSheet`, `UnifiedTabs` | `hasSelectedTab(..)`, `hasTabs(..)` |
| `UnifiedDetails` | `isOpen()`, `isClosed()`, `hasSummaryText(..)` |
| `UnifiedGrid` | `hasRowCount(..)`, `isEmpty()` |
| grid row | `hasCellText(column, text)`, `isSelected()`, `isNotSelected()`, `hasDetailsOpen()`, `hasDetailsClosed()` |
| grid column | `hasCellTexts(..)`, `isSortedAscending()`, `isSortedDescending()` |
| `UnifiedPage` | `isAt(View.class)`, `hasPath(..)`, `hasQueryParameter(..)` |

## Getters don't retry

`getValue()`, `getText()`, `getItems()`, `exists()` and `all()` read the state **now**. Use them to get a value you need, not to check one:

```java
assertThat(name).hasValue("Alice");                // good: retries
assertEquals("Alice", name.getValue());            // flaky in a browser after an async update
```

## Waiting for anything else

`page.waitUntil(..)` polls a condition with the same timeout. Use it for what no assertion covers, such as a value of your own wrapper:

```java
--8<-- "unified-test-demo/src/test/java/demo/AddonTest.java:wrapperWithoutOverride"
```

On Browserless, retrying assertions and `waitUntil` also run pending `UI.access` tasks and signal effects between attempts, so views updated from background threads behave as in a browser.

## Timeouts

| Scope | How |
|---|---|
| the whole run | `vaadin.testing.timeout=20s` in `junit-platform.properties`, or `-Dvaadin.testing.timeout=20s` |
| one wrapper | `wrapper.withTimeout(Duration.ofSeconds(30))` |

`withTimeout` returns a `UnifiedWrapper`; cast it back to use the wrapper's own methods:

```java
UnifiedButton slow = (UnifiedButton) page.find(UnifiedButton.class).withText("Export").single()
        .withTimeout(Duration.ofSeconds(30));
slow.click();
```

## Checking that something is gone

A query includes hidden components. To check that a component is no longer shown:

```java
assertThat(approve).isHidden();                                                  // still in the page, hidden
assertThat(dialog).isClosed();                                                   // dialogs and notifications
assertFalse(page.find(UnifiedButton.class).withText("Approve").visible().exists()); // right now
```
