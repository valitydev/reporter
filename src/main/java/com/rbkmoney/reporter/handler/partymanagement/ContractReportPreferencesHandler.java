package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.domain.ReportPreferences;
import com.rbkmoney.damsel.domain.ServiceAcceptanceActPreferences;
import com.rbkmoney.damsel.payment_processing.ContractEffect;
import com.rbkmoney.damsel.payment_processing.ContractEffectUnit;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractReportPreferencesHandler implements EventHandler<ContractEffectUnit> {

    private final TaskService taskService;

    @Override
    public void handle(MachineEvent event, ContractEffectUnit contractEffectUnit) {
        String partyId = event.getSourceId();
        String contractId = contractEffectUnit.getContractId();
        long eventId = event.getEventId();
        ContractEffect contractEffect = contractEffectUnit.getEffect();

        ReportPreferences reportPreferences = contractEffect.getReportPreferencesChanged();
        if (reportPreferences.isSetServiceAcceptanceActPreferences()) {
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
            taskService.deregisterProvisionOfServiceJob(partyId, contractId);
        }
    }
}
