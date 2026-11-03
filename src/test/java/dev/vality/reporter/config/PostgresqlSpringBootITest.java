package dev.vality.reporter.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(initializers = EmbeddedPostgresInitializer.class)
@ExtendWith(DatabaseCleanupExtension.class)
@MockitoSharedServices
@SpringBootTest
public @interface PostgresqlSpringBootITest {
}
