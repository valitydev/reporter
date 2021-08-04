package com.rbkmoney.reporter.service.invoicing;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.config.properties.FaultyEventsProperties;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.FaultyEventsService;
import com.rbkmoney.reporter.service.impl.InvoicingFaultyEventsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.yml")
@DirtiesContext
@EnableConfigurationProperties(FaultyEventsProperties.class)
@ContextConfiguration(
        classes = {
                FaultyEventsProperties.class,
                InvoicingFaultyEventsServiceImpl.class},
        initializers = InvoicingFaultyEventsServiceTest.Initializer.class)
public class InvoicingFaultyEventsServiceTest {

    private static final String TOPIC_NAME = "testTopic";
    private static final int PARTITION_ID = 1;

    @Autowired
    private FaultyEventsService invoicingFaultyEventsServiceImpl;

    @Test
    public void faultyEventsTest() {
        KafkaEvent kafkaEventOne = new KafkaEvent(TOPIC_NAME, PARTITION_ID, 1, new MachineEvent());
        assertTrue(invoicingFaultyEventsServiceImpl.isFaultyEvent(kafkaEventOne));

        KafkaEvent kafkaEventTwo = new KafkaEvent(TOPIC_NAME, PARTITION_ID, 5, new MachineEvent());
        assertFalse(invoicingFaultyEventsServiceImpl.isFaultyEvent(kafkaEventTwo));
    }

    public static class Initializer extends ConfigDataApplicationContextInitializer {

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
                    "faulty-events.topics.[0].partitions.[0].events.[3].offset=4")
                    .applyTo(configurableApplicationContext);
        }
    }
}
