package com.vaadin.testing.unified.selenium;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.Keys;

/** Playwright key names ({@code "Enter"}, {@code "Control+s"}) to Selenium key sequences. */
final class SeleniumKeys {
    private SeleniumKeys() {}

    static CharSequence chord(String chord) {
        String[] parts = chord.split("\\+(?!$)");
        List<CharSequence> keys = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) {
            keys.add(switch (parts[i]) {
                case "Control", "Ctrl" -> Keys.CONTROL;
                case "Shift" -> Keys.SHIFT;
                case "Alt" -> Keys.ALT;
                case "Meta", "Command" -> Keys.META;
                default -> throw new IllegalArgumentException("Unknown modifier \"" + parts[i] + "\" in \"" + chord + "\"");
            });
        }
        keys.add(key(parts[parts.length - 1]));
        return keys.size() == 1 ? keys.get(0) : Keys.chord(keys.toArray(CharSequence[]::new));
    }

    private static CharSequence key(String name) {
        if (name.length() == 1) {
            return name;
        }
        return switch (name) {
            case "Space" -> Keys.SPACE;
            case "Backspace" -> Keys.BACK_SPACE;
            case "ArrowUp" -> Keys.ARROW_UP;
            case "ArrowDown" -> Keys.ARROW_DOWN;
            case "ArrowLeft" -> Keys.ARROW_LEFT;
            case "ArrowRight" -> Keys.ARROW_RIGHT;
            case "PageUp" -> Keys.PAGE_UP;
            case "PageDown" -> Keys.PAGE_DOWN;
            default -> {
                // Enter, Escape, Tab, Delete, Home, End, Insert, F1..F12
                try {
                    yield Keys.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unknown key \"" + name + "\"", e);
                }
            }
        };
    }
}
