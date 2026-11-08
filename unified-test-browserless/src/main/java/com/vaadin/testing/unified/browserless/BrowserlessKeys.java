package com.vaadin.testing.unified.browserless;

import com.vaadin.browserless.internal.BasicUtilsKt;
import com.vaadin.browserless.internal.ShortcutsKt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keyboard and DOM events on Browserless. A browser dispatches a key press to the focused element and the
 * event bubbles up to the UI; here the event is fired on the target and on each of its ancestors, so key
 * listeners and shortcuts registered anywhere on the way ({@code addKeyDownListener},
 * {@code addClickShortcut}, {@code Shortcuts.addShortcutListener(..).listenOn(..)}) react.
 * <p>
 * Keys use Playwright's names: {@code "Enter"}, {@code "Escape"}, {@code "a"}, {@code "Control+S"}.
 */
final class BrowserlessKeys {
    private BrowserlessKeys() {}

    /** A key chord: the key and its modifiers. */
    record Chord(Key key, Key[] modifiers) {}

    static Chord parse(String chord) {
        String[] parts = chord.split("\\+(?!$)");
        List<Key> modifiers = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) {
            modifiers.add(switch (parts[i]) {
                case "Control", "Ctrl" -> KeyModifier.CONTROL;
                case "Shift" -> KeyModifier.SHIFT;
                case "Alt" -> KeyModifier.ALT;
                case "Meta", "Command" -> KeyModifier.META;
                default -> throw new IllegalArgumentException("Unknown modifier \"" + parts[i] + "\" in \"" + chord + "\"");
            });
        }
        return new Chord(key(parts[parts.length - 1]), modifiers.toArray(Key[]::new));
    }

    /**
     * Flow's constant for a key name, so it matches shortcuts registered with it: letters and digits are
     * key codes in Flow ({@code Key.KEY_S} is {@code "KeyS"}), named keys are key values ({@code "Enter"}).
     */
    static Key key(String name) {
        if (name.length() == 1 && Character.isLetter(name.charAt(0))) {
            return constant("KEY_" + name.toUpperCase());
        }
        if (name.length() == 1 && Character.isDigit(name.charAt(0))) {
            return constant("DIGIT_" + name);
        }
        if (" ".equals(name)) {
            return Key.SPACE;
        }
        for (Field f : Key.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == Key.class) {
                Key k = constant(f.getName());
                if (k.getKeys().contains(name)) {
                    return k;
                }
            }
        }
        return Key.of(name);
    }

    private static Key constant(String field) {
        try {
            return (Key) Key.class.getField(field).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Unknown key " + field, e);
        }
    }

    /** Fires keydown for the chord on the target and its ancestors, as the browser's event bubbling would. */
    static void press(Component target, String chord) {
        Chord parsed = parse(chord);
        for (Component c : path(target)) {
            ShortcutsKt._fireShortcut(c, parsed.key(), parsed.modifiers());
        }
    }

    /** Fires the DOM event on the target and its ancestors (Playwright's dispatchEvent bubbles too). */
    static void dispatch(Component target, String eventName) {
        for (Component c : path(target)) {
            BasicUtilsKt._fireDomEvent(c, eventName);
        }
    }

    private static List<Component> path(Component target) {
        List<Component> path = new ArrayList<>();
        for (Component c = target; c != null; c = c.getParent().orElse(null)) {
            path.add(c);
        }
        return path;
    }

    /**
     * Where a page-level key press goes. With a dialog open: the focused component inside it, else the dialog
     * (a modal dialog takes the keyboard, and Escape closes it wherever focus is). Otherwise the focused
     * component, else the UI.
     */
    static Component pageTarget(UI ui, List<Dialog> openDialogs) {
        Optional<Component> focused = focused(ui);
        if (!openDialogs.isEmpty()) {
            Dialog top = openDialogs.get(openDialogs.size() - 1);
            return focused.filter(f -> isInside(f, top)).orElse(top);
        }
        return focused.orElse(ui);
    }

    private static boolean isInside(Component c, Component ancestor) {
        for (Component p = c; p != null; p = p.getParent().orElse(null)) {
            if (p == ancestor) {
                return true;
            }
        }
        return false;
    }

    /** Browserless's own focus tracking (package-private there): what focus() and the testers last focused. */
    @SuppressWarnings("unchecked")
    private static Optional<Component> focused(UI ui) {
        try {
            Method m = Class.forName("com.vaadin.browserless.FocusTracker")
                    .getDeclaredMethod("getFocusedComponent", UI.class);
            m.setAccessible(true);
            return ((Optional<Component>) m.invoke(null, ui)).filter(Component::isAttached);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
