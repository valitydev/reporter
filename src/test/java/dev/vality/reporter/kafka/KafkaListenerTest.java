package dev.vality.reporter.kafka;

import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.reporter.config.MockitoSharedServices;
import dev.vality.reporter.model.KafkaEvent;
import dev.vality.reporter.service.impl.InvoicingService;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainerSingleton;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.thrift.TBase;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = "reporter-invoicing-test")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=reporter-kafka-test",
        "kafka.topics.invoicing.id=reporter-invoicing-test",
        "kafka.topics.invoicing.enabled=true",
        "kafka.consumer.max-poll-records=1"
})
@MockitoSharedServices
@PostgresqlTestcontainerSingleton(excludeTruncateTables = "schema_version")
class KafkaListenerTest {

    @Value("${kafka.topics.invoicing.id}")
    public String invoicingTopic;

    @MockitoBean
    private InvoicingService invoicingService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Captor
    private ArgumentCaptor<List<KafkaEvent>> arg;

    @BeforeEach
    void waitForKafkaListenersAssignment() {
        for (MessageListenerContainer listenerContainer : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(listenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @Test
    void listenInvoicingChanges() throws Exception {
        int eventsCount = 1;
        for (int i = 0; i < eventsCount; i++) {
            send(invoicingTopic, createSinkEvent());
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

    private void send(String topic, TBase<?, ?> event) throws Exception {
        try (var producer = new KafkaProducer<String, byte[]>(producerProperties())) {
            var serializer = new TSerializer(new TBinaryProtocol.Factory());
            producer.send(new ProducerRecord<>(topic, serializer.serialize(event))).get();
            producer.flush();
        }
    }

    private Properties producerProperties() {
        var properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        return properties;
    }
}
