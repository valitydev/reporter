package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyManagementEventHandler implements EventHandler<PartyEventData> {

    private final EventHandler<ContractEffectUnit> contractEffectHander;

    @Override
    public void handle(MachineEvent event, PartyEventData partyEventData) {
        partyEventData.getChanges()
                .forEach(partyChange -> processPartyChange(event, partyChange));
    }

    private void processPartyChange(MachineEvent event, PartyChange partyChange) {
        if (partyChange.isSetClaimStatusChanged()) {
            ClaimStatus claimStatus = partyChange.getClaimStatusChanged().getStatus();
            processClaimStatusChange(event, claimStatus);
        }
    }

    private void processClaimStatusChange(MachineEvent event, ClaimStatus claimStatus) {
        if (claimStatus.isSetAccepted()) {
            claimStatus.getAccepted().getEffects().stream()
                    .filter(ClaimEffect::isSetContractEffect)
                    .forEach(claimEffect ->
                            contractEffectHander.handle(event, claimEffect.getContractEffect()));
        }
    }
}
