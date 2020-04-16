package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.domain.Contract;
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
public class ContractCreatedChangesHandler implements EventHandler<ContractEffectUnit> {

    private final TaskService taskService;

    @Override
    public void handle(MachineEvent event, ContractEffectUnit contractEffectUnit) {
        String partyId = event.getSourceId();
        String contractId = contractEffectUnit.getContractId();
        long eventId = event.getEventId();
        ContractEffect contractEffect = contractEffectUnit.getEffect();

        Contract contract = contractEffect.getCreated();
        if (contract.isSetReportPreferences()) {
            ReportPreferences preferences = contract.getReportPreferences();
            if (preferences.isSetServiceAcceptanceActPreferences()) {
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

}
