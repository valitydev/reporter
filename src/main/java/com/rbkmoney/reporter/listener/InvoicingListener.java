package com.rbkmoney.reporter.listener;

import com.rbkmoney.kafka.common.util.LogUtil;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InvoicingListener {

    private final EventService invoicingService;

    @Value("${kafka.topics.invoicing.error-throttling-timeout-ms}")
    private int errorThrottlingTimeout;

    @KafkaListener(topics = "${kafka.topics.invoicing.id}",
            containerFactory = "kafkaInvoicingListenerContainerFactory")
    public void listen(List<ConsumerRecord<String, SinkEvent>> messages, Acknowledgment ack) throws Exception {
        log.info("Got invoicing machineEvent batch with size: {}", messages.size());
        try {
            invoicingService.handleEvents(
                    messages.stream()
                            .map(message -> new KafkaEvent(
                                    message.topic(),
                                    message.partition(),
                                    message.offset(),
                                    message.value().getEvent())
                            )
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            log.error("Received error during processing invoice batch: ", e);
            Thread.sleep(errorThrottlingTimeout);
            throw e;
        }
        ack.acknowledge();
        log.info("Invoicing batch has been committed, size={}, {}", messages.size(),
                LogUtil.toSummaryStringWithSinkEventValues(messages));
    }

}
