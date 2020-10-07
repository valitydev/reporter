package com.rbkmoney.reporter.config;

import com.rbkmoney.easyway.AbstractTestUtils;
import com.rbkmoney.reporter.config.properties.FaultyEventsProperties;
import com.rbkmoney.reporter.service.impl.InvoicingFaultyEventsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(FaultyEventsProperties.class)
@ContextConfiguration(
        classes = {
                FaultyEventsProperties.class,
                InvoicingFaultyEventsServiceImpl.class,
        },
        initializers = AbstractInvoicingFaultyEventsServiceConfig.Initializer.class
)
@TestPropertySource("classpath:application.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public abstract class AbstractInvoicingFaultyEventsServiceConfig extends AbstractTestUtils {

    public static final String TOPIC_NAME = "testTopic";
    public static final int PARTITION_ID = 1;

    public static class Initializer extends ConfigFileApplicationContextInitializer {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            super.initialize(configurableApplicationContext);
            TestPropertyValues.of(
                    "kafka.topics.invoicing.id=" + "TOPIC_NAME",
                    "faulty-events.topics.[0].topicName=" + "TOPIC_NAME",
                    "faulty-events.topics.[0].partitions.[0].partitionNumber=" + 1,
                    "faulty-events.topics.[0].partitions.[0].events.[0].offset=1",
                    "faulty-events.topics.[0].partitions.[0].events.[1].offset=2",
                    "faulty-events.topics.[0].partitions.[0].events.[2].offset=3",
                    "faulty-events.topics.[0].partitions.[0].events.[3].offset=4"
            )
                    .applyTo(configurableApplicationContext);
        }
    }

}
