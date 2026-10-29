package ar.com.pasalagorra;

import ar.com.pasalagorra.config.AsyncSyncConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { PasalagorraApp.class, AsyncSyncConfiguration.class, ar.com.pasalagorra.config.JacksonHibernateConfiguration.class }
)
public @interface IntegrationTest {}
