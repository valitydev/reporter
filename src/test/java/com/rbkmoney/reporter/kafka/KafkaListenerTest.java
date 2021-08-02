package com.rbkmoney.reporter.kafka;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.reporter.config.KafkaPostgresqlSpringBootITest;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.impl.InvoicingService;
import com.rbkmoney.testcontainers.annotations.kafka.config.KafkaProducer;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@KafkaPostgresqlSpringBootITest
public class KafkaListenerTest {

    @Value("${kafka.topics.invoicing.id}")
    public String invoicingTopic;

    @MockBean
    private InvoicingService invoicingService;

    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Captor
    private ArgumentCaptor<List<KafkaEvent>> arg;

    @Test
    public void listenInvoicingChanges() throws Exception {
        int eventsCount = 5;
        for (int i = 0; i < eventsCount; i++) {
            testThriftKafkaProducer.send(invoicingTopic, createSinkEvent());
        }
        verify(invoicingService, timeout(10000).times(1))
                .handleEvents(arg.capture());
        assertThat(arg.getValue().size())
                .isEqualTo(eventsCount);
    }

    private SinkEvent createSinkEvent() {
        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(createMessage());
        return sinkEvent;
    }

    private MachineEvent createMessage() {
        MachineEvent message = new MachineEvent();
        var data = new com.rbkmoney.machinegun.msgpack.Value();
        data.setBin(new byte[0]);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        message.setEventId(1L);
        message.setSourceNs("sad");
        message.setSourceId("sda");
        message.setData(data);
        return message;
    }
}
