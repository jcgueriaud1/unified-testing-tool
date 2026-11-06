// PROTOTYPE: injects UnifiedPage; adapter chosen by the run's Testing Tool. No base class (#9).
package demo.unified;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class UnifiedExtension implements ParameterResolver, AfterEachCallback {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(UnifiedExtension.class);

    @Override
    public boolean supportsParameter(ParameterContext p, ExtensionContext c) {
        return p.getParameter().getType() == UnifiedPage.class;
    }

    @Override
    public Object resolveParameter(ParameterContext p, ExtensionContext c) {
        ApplicationContext spring = SpringExtension.getApplicationContext(c);
        UnifiedPage page = switch (TestingTool.current()) {
            case BROWSERLESS -> new BrowserlessPage(spring, c.getRequiredTestClass());
            case PLAYWRIGHT -> new PlaywrightPage(
                    "http://localhost:" + spring.getEnvironment().getRequiredProperty("local.server.port"));
        };
        c.getStore(NS).put("page", page);
        return page;
    }

    @Override
    public void afterEach(ExtensionContext c) {
        UnifiedPage page = c.getStore(NS).remove("page", UnifiedPage.class);
        if (page != null) {
            page.close();
        }
    }
}
