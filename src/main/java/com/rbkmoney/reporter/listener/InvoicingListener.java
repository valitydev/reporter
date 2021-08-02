package com.rbkmoney.reporter.listener;

import com.rbkmoney.kafka.common.util.LogUtil;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InvoicingListener {

    private final EventService invoicingService;

    @KafkaListener(
            autoStartup = "${kafka.topics.invoicing.enabled}",
            topics = "${kafka.topics.invoicing.id}",
            containerFactory = "kafkaInvoicingListenerContainerFactory")
    public void listen(List<ConsumerRecord<String, SinkEvent>> messages, Acknowledgment ack) throws Exception {
        int batchSize = messages.size();
        log.debug("Got invoicing machineEvent batch with size: {}", batchSize);
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
        } catch (Throwable ex) {
            log.error("Received error during processing invoice batch (size: {}, summary: {})", batchSize,
                    LogUtil.toSummaryStringWithSinkEventValues(messages), ex);
            throw ex;
        }
        ack.acknowledge();
        log.debug("Invoicing batch has been committed, size={}, {}", batchSize,
                LogUtil.toSummaryStringWithSinkEventValues(messages));
    }
}
