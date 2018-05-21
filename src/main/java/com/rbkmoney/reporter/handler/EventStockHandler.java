package com.rbkmoney.reporter.handler;

import com.rbkmoney.damsel.domain.Contract;
import com.rbkmoney.damsel.domain.ReportPreferences;
import com.rbkmoney.damsel.domain.ServiceAcceptanceActPreferences;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.reporter.exception.StorageException;
import com.rbkmoney.reporter.service.TaskService;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventStockHandler implements EventHandler<StockEvent> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final TaskService taskService;

    @Autowired
    public EventStockHandler(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public EventAction handle(StockEvent stockEvent, String subsKey) {
        try {
            Event event = stockEvent
                    .getSourceEvent()
                    .getProcessingEvent();

            if(event.getSource().isSetPartyId()) {
                String partyId = event.getSource().getPartyId();
                List<PartyChange> partyChanges = event
                        .getPayload()
                        .getPartyChanges();

                for (PartyChange partyChange : partyChanges) {
                    if (partyChange.isSetClaimStatusChanged()) {
                        ClaimStatus claimStatus = partyChange.getClaimStatusChanged().getStatus();
                        if (claimStatus.isSetAccepted()) {
                            for (ClaimEffect claimEffect : claimStatus.getAccepted().getEffects()) {
                                if (claimEffect.isSetContractEffect()) {
                                    ContractEffectUnit contractEffectUnit = claimEffect.getContractEffect();
                                    String contractId = contractEffectUnit.getContractId();
                                    ContractEffect contractEffect = contractEffectUnit.getEffect();
                                    handleContractEffect(partyId, contractId, event.getId(), contractEffect);
                                }
                            }
                        }
                    }
                }
            }
            return EventAction.CONTINUE;
        } catch (StorageException | WRuntimeException ex) {
            log.warn("Failed to handle event, retry", ex);
            return EventAction.DELAYED_RETRY;
        }
    }

    private void handleContractEffect(String partyId, String contractId, long eventId, ContractEffect contractEffect) {
        if (contractEffect.isSetCreated()) {
            Contract contract = contractEffect.getCreated();
            if (contract.isSetReportPreferences()) {
                ReportPreferences preferences = contract.getReportPreferences();
                if (preferences.isSetServiceAcceptanceActPreferences()) {
                    handlePreferences(partyId, contractId, eventId, preferences.getServiceAcceptanceActPreferences());
                }
            }
        } else if (contractEffect.isSetReportPreferencesChanged()) {
            ReportPreferences reportPreferences = contractEffect.getReportPreferencesChanged();
            if (reportPreferences.isSetServiceAcceptanceActPreferences()) {
                ServiceAcceptanceActPreferences preferences = reportPreferences.getServiceAcceptanceActPreferences();
                handlePreferences(partyId, contractId, eventId, preferences);
            } else {
                taskService.deregisterProvisionOfServiceJob(partyId, contractId);
            }
        }
    }

    private void handlePreferences(String partyId, String contractId, long eventId, ServiceAcceptanceActPreferences preferences) {
        taskService.registerProvisionOfServiceJob(
                partyId,
                contractId,
                eventId,
                preferences.getSchedule(),
                preferences.getSigner()
        );
    }

}
