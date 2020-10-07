package com.rbkmoney.reporter.service.invoicing;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.config.AbstractInvoicingFaultyEventsServiceConfig;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.FaultyEventsService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvoicingFaultyEventsServiceTest extends AbstractInvoicingFaultyEventsServiceConfig {

    @Autowired
    private FaultyEventsService invoicingFaultyEventsServiceImpl;

    @Test
    public void faultyEventsTest() {
        KafkaEvent kafkaEventOne = new KafkaEvent(TOPIC_NAME, PARTITION_ID, 1, new MachineEvent());
        assertTrue(invoicingFaultyEventsServiceImpl.isFaultyEvent(kafkaEventOne));

        KafkaEvent kafkaEventTwo = new KafkaEvent(TOPIC_NAME, PARTITION_ID, 5, new MachineEvent());
        assertFalse(invoicingFaultyEventsServiceImpl.isFaultyEvent(kafkaEventTwo));
    }

}
