package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.domain.ReportPreferences;
import com.rbkmoney.damsel.domain.ServiceAcceptanceActPreferences;
import com.rbkmoney.damsel.payment_processing.ContractEffect;
import com.rbkmoney.damsel.payment_processing.ContractEffectUnit;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.handler.EventHandler;
import com.rbkmoney.reporter.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractReportPreferencesHandler implements JobRegistratorEventHandler {

    private final TaskService taskService;

    @Override
    public void handle(MachineEvent event, ContractEffectUnit contractEffectUnit, int changeId) {
        String partyId = event.getSourceId();
        String contractId = contractEffectUnit.getContractId();
        long eventId = event.getEventId();
        ContractEffect contractEffect = contractEffectUnit.getEffect();

        ReportPreferences reportPreferences = contractEffect.getReportPreferencesChanged();
        if (reportPreferences.isSetServiceAcceptanceActPreferences()) {
            log.info("Register job by created changes (party id = '{}', contract id = '{}', event id ='{}', " +
                            "change id = '{}')", partyId, contractId, eventId, changeId);
            ServiceAcceptanceActPreferences preferences =
                    reportPreferences.getServiceAcceptanceActPreferences();
            taskService.registerProvisionOfServiceJob(
                    partyId,
                    contractId,
                    eventId,
                    preferences.getSchedule(),
                    preferences.getSigner()
            );
        } else {
            log.info("Deregister job by created changes (party id = '{}', contract id = '{}', " +
                            "event id ='{}', change id = '{}')", partyId, contractId, eventId, changeId);
            taskService.deregisterProvisionOfServiceJob(partyId, contractId);
        }
    }

    @Override
    public boolean isAccept(ContractEffectUnit contractEffectUnit) {
        return contractEffectUnit.isSetEffect()
                && contractEffectUnit.getEffect().isSetReportPreferencesChanged();
    }
}
