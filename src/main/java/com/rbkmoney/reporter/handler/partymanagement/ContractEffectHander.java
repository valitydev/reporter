package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.payment_processing.ContractEffect;
import com.rbkmoney.damsel.payment_processing.ContractEffectUnit;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractEffectHander implements EventHandler<ContractEffectUnit> {

    private final EventHandler<ContractEffectUnit> contractCreatedChangesHandler;

    private final EventHandler<ContractEffectUnit> contractReportPreferencesHandler;

    @Override
    public void handle(MachineEvent event, ContractEffectUnit contractEffectUnit) {
        ContractEffect contractEffect = contractEffectUnit.getEffect();

        if (contractEffect.isSetCreated()) {
            contractCreatedChangesHandler.handle(event, contractEffectUnit);
        } else if (contractEffect.isSetReportPreferencesChanged()) {
            contractReportPreferencesHandler.handle(event, contractEffectUnit);
        }
    }
}
