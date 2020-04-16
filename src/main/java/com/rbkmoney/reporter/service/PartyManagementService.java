package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.exception.StorageException;
import com.rbkmoney.reporter.handler.partymanagement.EventHandler;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyManagementService {

    private final MachineEventParser<PartyEventData> parser;

    private final EventHandler<PartyEventData> partyManagementEventHandler;

    @Transactional(propagation = Propagation.REQUIRED)
    public void handleEvents(List<MachineEvent> machineEvents) {
        machineEvents.forEach(this::handleIfAccept);
    }

    private void handleIfAccept(MachineEvent machineEvent) {
        PartyEventData eventPayload = parser.parse(machineEvent);
        try {
            if (eventPayload.isSetChanges()) {
                partyManagementEventHandler.handle(machineEvent, eventPayload);
            }
        } catch (StorageException | WRuntimeException ex) {
            log.warn("Failed to handle event, retry", ex);
            throw ex;
        }
    }

}
