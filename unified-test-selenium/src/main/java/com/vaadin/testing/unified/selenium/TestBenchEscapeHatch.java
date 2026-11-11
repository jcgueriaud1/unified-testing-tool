package com.vaadin.testing.unified.selenium;

import com.vaadin.testbench.AbstractBrowserDriverTestBase;
import com.vaadin.testing.unified.UnifiedPage;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * Escape Hatch to TestBench's test base API ({@code $(..)}, {@code executeScript}, {@code waitUntil},
 * {@code testBench()}, ...) on a Unified Page's browser. The TestBench Migration Recipe routes the calls it
 * can't convert through it, so a migrated test keeps compiling and running on Selenium:
 *
 * <pre>{@code
 * @Test
 * @RequiresTestingTool(TestingTool.SELENIUM)
 * void countsPeople(UnifiedPage page) {
 *     TestBenchEscapeHatch testBench = TestBenchEscapeHatch.of(page);
 *     int rows = testBench.$(GridElement.class).first().getRowCount();
 * }
 * }</pre>
 *
 * Selenium only. As inside any Escape Hatch, no Unified API guarantees apply: no Auto-Wait, no lazy lookup,
 * no retrying assertions. The protected methods of TestBench's base class are public here.
 */
public final class TestBenchEscapeHatch extends AbstractBrowserDriverTestBase {

    private TestBenchEscapeHatch(WebDriver driver) {
        setDriver(driver);
    }

    /** TestBench's test base API on the page's browser; fails when the test does not run on Selenium. */
    public static TestBenchEscapeHatch of(UnifiedPage page) {
        return new TestBenchEscapeHatch(page.unwrap(WebDriver.class));
    }

    public static void assertEquals(WebElement expected, WebElement actual) {
        AbstractBrowserDriverTestBase.assertEquals(expected, actual);
    }

    @Override
    public <T> void waitUntilNot(ExpectedCondition<T> condition) {
        super.waitUntilNot(condition);
    }

    @Override
    public <T> void waitUntilNot(ExpectedCondition<T> condition, long timeoutInSeconds) {
        super.waitUntilNot(condition, timeoutInSeconds);
    }

    @Override
    public boolean hasCssClass(WebElement element, String className) {
        return super.hasCssClass(element, className);
    }

    @Override
    public int getScrollX() {
        return super.getScrollX();
    }

    @Override
    public int getScrollY() {
        return super.getScrollY();
    }

    @Override
    public List<LogEntry> getLogEntries(Level level) {
        return super.getLogEntries(level);
    }

    @Override
    public void checkLogsForErrors() {
        super.checkLogsForErrors();
    }

    @Override
    public void checkLogsForErrors(Predicate<String> acceptableMessagePredicate) {
        super.checkLogsForErrors(acceptableMessagePredicate);
    }

    @Override
    public void clickElementWithJs(String elementId) {
        super.clickElementWithJs(elementId);
    }

    @Override
    public void clickElementWithJs(WebElement element) {
        super.clickElementWithJs(element);
    }

    @Override
    public void scrollBy(int x, int y) {
        super.scrollBy(x, y);
    }

    @Override
    public void scrollIntoViewAndClick(WebElement element) {
        super.scrollIntoViewAndClick(element);
    }

    @Override
    public void scrollToElement(WebElement element) {
        super.scrollToElement(element);
    }

    @Override
    public void waitForDevServer() {
        super.waitForDevServer();
    }

    @Override
    public void waitForElementNotPresent(By by) {
        super.waitForElementNotPresent(by);
    }

    @Override
    public void waitForElementPresent(By by) {
        super.waitForElementPresent(by);
    }

    @Override
    public void waitForElementVisible(By by) {
        super.waitForElementVisible(by);
    }
}
