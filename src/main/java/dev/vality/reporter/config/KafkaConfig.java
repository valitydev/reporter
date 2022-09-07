package dev.vality.reporter.config;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.kafka.common.util.ExponentialBackOffDefaultErrorHandlerFactory;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.serde.SinkEventDeserializer;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import dev.vality.sink.common.parser.impl.PaymentEventPayloadMachineEventParser;
import dev.vality.sink.common.serialization.BinaryDeserializer;
import dev.vality.sink.common.serialization.impl.PaymentEventPayloadDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

@SuppressWarnings("LineLength")
@EnableKafka
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Value("${kafka.topics.invoicing.concurrency}")
    private int invoicingConcurrency;

    @Bean
    public BinaryDeserializer<EventPayload> paymentEventPayloadDeserializer() {
        return new PaymentEventPayloadDeserializer();
    }

    @Bean
    public MachineEventParser<EventPayload> paymentMachineEventParser(
            BinaryDeserializer<EventPayload> paymentEventPayloadDeserializer) {
        return new PaymentEventPayloadMachineEventParser(paymentEventPayloadDeserializer);
    }

    @Bean
    public ConsumerFactory<String, MachineEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(getConfigs());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, MachineEvent>> kafkaInvoicingListenerContainerFactory(ConsumerFactory<String, MachineEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MachineEvent> factory = createDefaultListenerContainerFactory(consumerFactory);
        factory.setConcurrency(invoicingConcurrency);
        return factory;
    }

    public ConcurrentKafkaListenerContainerFactory<String, MachineEvent> createDefaultListenerContainerFactory(ConsumerFactory<String, MachineEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MachineEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(ExponentialBackOffDefaultErrorHandlerFactory.create());
        return factory;
    }

    private Map<String, Object> getConfigs() {
        var properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SinkEventDeserializer.class);
        return properties;
    }
}
