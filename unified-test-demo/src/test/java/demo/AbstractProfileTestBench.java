package demo;

import com.vaadin.testbench.BrowserTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Golden migration of a TestBench base type, a test extending it and a page object (includePageObjects). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractProfileTestBench extends BrowserTestBase {

    @LocalServerPort
    int port;

    @BeforeEach
    void openProfile() {
        open("profile");
    }

    void open(String path) {
        getDriver().get("http://localhost:" + port + "/" + path);
    }
}
