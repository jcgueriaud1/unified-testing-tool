package demo;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class GreetingService {
    /** Counts Spring contexts created in this JVM (one instance per context). */
    public static final AtomicInteger INSTANCES = new AtomicInteger();

    public GreetingService() {
        INSTANCES.incrementAndGet();
    }

    public String greet(String name) {
        return "Hello " + name;
    }
}
