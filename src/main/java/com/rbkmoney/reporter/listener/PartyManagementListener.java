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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PartyManagementListener {

    private final EventService partyManagementService;

    @KafkaListener(topics = "${kafka.topics.party-management.id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handle(List<ConsumerRecord<String, SinkEvent>> messages, Acknowledgment ack) throws Exception {
        log.info("Got partyManagement machineEvent batch with size: {}", messages.size());
        partyManagementService.handleEvents(
                messages.stream()
                        .map(message -> new KafkaEvent(
                                message.topic(),
                                message.partition(),
                                message.offset(),
                                message.value().getEvent())
                        )
                        .collect(Collectors.toList())
        );
        ack.acknowledge();
        log.info("Batch partyManagement has been committed, size={}, {}", messages.size(),
                LogUtil.toSummaryStringWithSinkEventValues(messages));
    }
}