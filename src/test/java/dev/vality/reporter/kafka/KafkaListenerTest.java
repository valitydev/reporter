package dev.vality.reporter.kafka;

import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.reporter.config.MockitoSharedServices;
import dev.vality.reporter.model.KafkaEvent;
import dev.vality.reporter.service.impl.InvoicingService;
import dev.vality.testcontainers.annotations.KafkaConfig;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerSingleton;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainerSingleton;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@KafkaConfig
@DirtiesContext
@KafkaTestcontainerSingleton(
        properties = {
                "kafka.topics.invoicing.enabled=true",
                "kafka.consumer.max-poll-records=1"
        },
        topicsKeys = "kafka.topics.invoicing.id"
)
@MockitoSharedServices
@PostgresqlTestcontainerSingleton(excludeTruncateTables = "schema_version")
class KafkaListenerTest {

    @Value("${kafka.topics.invoicing.id}")
    public String invoicingTopic;

    @MockitoBean
    private InvoicingService invoicingService;

    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Captor
    private ArgumentCaptor<List<KafkaEvent>> arg;

    @Test
    void listenInvoicingChanges() throws Exception {
        int eventsCount = 1;
        for (int i = 0; i < eventsCount; i++) {
            testThriftKafkaProducer.send(invoicingTopic, createSinkEvent());
        }
        verify(invoicingService, timeout(50000).times(eventsCount))
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
        var data = new dev.vality.machinegun.msgpack.Value();
        data.setBin(new byte[0]);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        message.setEventId(1L);
        message.setSourceNs("sad");
        message.setSourceId("sda");
        message.setData(data);
        return message;
    }
}
