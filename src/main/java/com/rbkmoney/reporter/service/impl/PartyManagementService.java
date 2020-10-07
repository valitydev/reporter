package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.handler.EventHandler;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.EventService;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyManagementService implements EventService {

    private final MachineEventParser<PartyEventData> parser;
    private final List<EventHandler<PartyChange>> partyEventHandlers;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleEvents(List<KafkaEvent> kafkaEvents) throws Exception {
        for (KafkaEvent kafkaEvent : kafkaEvents) {
            MachineEvent machineEvent = kafkaEvent.getEvent();
            PartyEventData partyEventData = parser.parse(machineEvent);
            if (partyEventData.isSetChanges()) {
                List<PartyChange> changes = partyEventData.getChanges();
                for (int i = 0; i < changes.size(); i++) {
                    handleIfAccept(partyEventHandlers, kafkaEvent, changes.get(i), i);
                }
            }
        }
    }

    void handleIfAccept(List<EventHandler<PartyChange>> handlers,
                        KafkaEvent kafkaEvent,
                        PartyChange change,
                        int changeId) throws Exception {
        for (EventHandler<PartyChange> handler : handlers) {
            if (handler.isAccept(change)) {
                handler.handle(kafkaEvent, change, changeId);
            }
        }
    }

}
