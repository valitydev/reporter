package dev.vality.reporter.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ContextConfiguration(initializers = {EmbeddedPostgresInitializer.class, EmbeddedS3Initializer.class})
@ExtendWith(DatabaseCleanupExtension.class)
public @interface S3SpringBootITest {
}
