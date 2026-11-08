package com.vaadin.testing.unified.internal;

import com.vaadin.testing.unified.TestingTool;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.extension.ExtensionContext;

/** The Unified API's JUnit configuration parameters (also read from system properties). */
public final class Settings {
    private Settings() {}

    public static final String TOOL = "vaadin.testing.tool";
    public static final String BASE_URL = "vaadin.testing.base-url";
    public static final String TIMEOUT = "vaadin.testing.timeout";

    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*(ms|s|m)?");

    public static Optional<String> get(ExtensionContext context, String key) {
        return context.getConfigurationParameter(key).filter(v -> !v.isBlank());
    }

    public static TestingTool tool(ExtensionContext context) {
        return get(context, TOOL).map(TestingTool::parse).orElse(TestingTool.BROWSERLESS);
    }

    public static Duration timeout(ExtensionContext context) {
        return get(context, TIMEOUT).map(Settings::parseDuration).orElse(Duration.ofSeconds(10));
    }

    static Duration parseDuration(String value) {
        Matcher m = DURATION.matcher(value.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException(TIMEOUT + ": expected e.g. 10s, 500ms or 1m, got " + value);
        }
        long amount = Long.parseLong(m.group(1));
        String unit = m.group(2) == null ? "s" : m.group(2);
        return switch (unit) {
            case "ms" -> Duration.ofMillis(amount);
            case "m" -> Duration.ofMinutes(amount);
            default -> Duration.ofSeconds(amount);
        };
    }
}
