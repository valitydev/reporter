package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.domain.Contract;
import com.rbkmoney.damsel.domain.ReportPreferences;
import com.rbkmoney.damsel.domain.ServiceAcceptanceActPreferences;
import com.rbkmoney.damsel.payment_processing.ContractEffect;
import com.rbkmoney.damsel.payment_processing.ContractEffectUnit;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractCreatedChangesHandler implements JobRegistratorEventHandler {

    private final TaskService taskService;

    @Override
    public void handle(KafkaEvent kafkaEvent, ContractEffectUnit contractEffectUnit, int changeId) {
        MachineEvent event = kafkaEvent.getEvent();
        String partyId = event.getSourceId();
        String contractId = contractEffectUnit.getContractId();
        long eventId = event.getEventId();
        ContractEffect contractEffect = contractEffectUnit.getEffect();

        Contract contract = contractEffect.getCreated();
        if (contract.isSetReportPreferences()) {
            ReportPreferences preferences = contract.getReportPreferences();
            if (preferences.isSetServiceAcceptanceActPreferences()) {
                log.info("Register job by created changes (party id = '{}', " +
                                "contract id = '{}', event id ='{}', change id = '{}')", partyId, contractId,
                        eventId, changeId);
                ServiceAcceptanceActPreferences actPreferences =
                        preferences.getServiceAcceptanceActPreferences();
                taskService.registerProvisionOfServiceJob(
                        partyId,
                        contractId,
                        eventId,
                        actPreferences.getSchedule(),
                        actPreferences.getSigner()
                );
            }
        }
    }

    @Override
    public boolean isAccept(ContractEffectUnit contractEffectUnit) {
        return contractEffectUnit.isSetEffect()
                && contractEffectUnit.getEffect().isSetCreated();
    }

}
