package com.vaadin.testing.unified.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.testing.unified.TestingTool;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.component.UnifiedButton;
import com.vaadin.testing.unified.spi.AdapterSession;
import com.vaadin.testing.unified.spi.QuerySpec;
import com.vaadin.testing.unified.spi.StaleElementException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StaleRetryTest {

    @Test
    void findsAgainWhenAnElementGoesStale() {
        AtomicInteger finds = new AtomicInteger();
        AtomicInteger clicks = new AtomicInteger();
        UnifiedElement button = (UnifiedElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { UnifiedElement.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "isVisible", "isEnabled" -> true;
                    case "getText" -> "Reject";
                    case "click" -> {
                        if (clicks.incrementAndGet() == 1) {
                            throw new StaleElementException("re-rendered", null);
                        }
                        yield null;
                    }
                    default -> null;
                });
        AdapterSession session = (AdapterSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { AdapterSession.class }, (proxy, method, args) -> {
                    if (method.getName().equals("find")) {
                        if (finds.incrementAndGet() == 1) {
                            throw new StaleElementException("re-rendered during the filter", null);
                        }
                        assertEquals(UnifiedButton.class.getAnnotation(com.vaadin.testing.unified.UnifiedComponent.class)
                                .value(), ((QuerySpec) args[0]).componentType());
                        return List.of(button);
                    }
                    return null;
                });
        UnifiedPageImpl page = new UnifiedPageImpl(new TestRun(TestingTool.SELENIUM, session, Duration.ofSeconds(2)));

        boolean exists = page.find(UnifiedButton.class).withText("Reject").exists();
        page.find(UnifiedButton.class).withText("Reject").single().click();

        assertEquals(true, exists);
        assertEquals(2, clicks.get());
    }
}
