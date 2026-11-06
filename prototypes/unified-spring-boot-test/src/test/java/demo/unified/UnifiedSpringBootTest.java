// PROTOTYPE: @UnifiedSpringBootTest = Spring Boot test bootstrapping chosen per Testing Tool + Unified extension.
// Deliberately NOT meta-annotated with @SpringBootTest, so there is a single @BootstrapWith.
package demo.unified;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@BootstrapWith(UnifiedSpringBootTestContextBootstrapper.class)
@ExtendWith({ SpringExtension.class, UnifiedExtension.class })
public @interface UnifiedSpringBootTest {}
