package com.rbkmoney.reporter.config;

import com.rbkmoney.reporter.config.testconfiguration.MockedUnimportantServicesConfig;
import com.rbkmoney.testcontainers.annotations.KafkaSpringBootTest;
import com.rbkmoney.testcontainers.annotations.kafka.KafkaTestcontainerSingleton;
import com.rbkmoney.testcontainers.annotations.postgresql.PostgresqlTestcontainerSingleton;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PostgresqlTestcontainerSingleton
@KafkaTestcontainerSingleton(
        properties = {
                "kafka.topics.invoicing.enabled=true",
                "kafka.consumer.max-poll-records=5"},
        topicsKeys = "kafka.topics.invoicing.id")
@KafkaSpringBootTest
@Import(MockedUnimportantServicesConfig.class)
public @interface KafkaPostgresqlSpringBootITest {
}
