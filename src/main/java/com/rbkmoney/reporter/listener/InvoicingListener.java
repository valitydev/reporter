package com.rbkmoney.reporter.listener;

import com.rbkmoney.kafka.common.util.LogUtil;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.reporter.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InvoicingListener {

    private final EventService invoicingService;

    @KafkaListener(topics = "${kafka.topics.invoice.id}", containerFactory = "kafkaInvoicingListenerContainerFactory")
    public void listen(List<ConsumerRecord<String, SinkEvent>> messages, Acknowledgment ack) throws Exception {
        log.info("Got invoicing machineEvent batch with size: {}", messages.size());
        invoicingService.handleEvents(
                messages.stream()
                        .map(m -> m.value().getEvent())
                        .collect(Collectors.toList())
        );
        ack.acknowledge();
        log.info("Invoicing batch has been committed, size={}, {}", messages.size(),
                LogUtil.toSummaryStringWithSinkEventValues(messages));
    }

}
