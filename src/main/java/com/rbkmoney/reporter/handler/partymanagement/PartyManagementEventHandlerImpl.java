package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.damsel.payment_processing.ClaimAccepted;
import com.rbkmoney.damsel.payment_processing.ClaimEffect;
import com.rbkmoney.damsel.payment_processing.ContractEffectUnit;
import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.reporter.handler.EventHandler;
import com.rbkmoney.reporter.model.KafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PartyManagementEventHandlerImpl implements PartyManagementEventHandler {

    private final List<JobRegistratorEventHandler> eventHandlers;

    @Override
    public void handle(KafkaEvent kafkaEvent, PartyChange change, int changeId) {
        ClaimAccepted accepted = change.getClaimStatusChanged().getStatus().getAccepted();
        accepted.getEffects().stream()
                .filter(ClaimEffect::isSetContractEffect)
                .map(ClaimEffect::getContractEffect)
                .forEach(contractEffect -> handleContractEffect(kafkaEvent, contractEffect, changeId));
    }

    private void handleContractEffect(KafkaEvent kafkaEvent, ContractEffectUnit contractEffect, int changeId) {
        for (EventHandler<ContractEffectUnit> handler : eventHandlers) {
            if (handler.isAccept(contractEffect)) {
                try {
                    handler.handle(kafkaEvent, contractEffect, changeId);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @Override
    public boolean isAccept(PartyChange change) {
        return change.isSetClaimStatusChanged()
                && change.getClaimStatusChanged().isSetStatus()
                && change.getClaimStatusChanged().getStatus().isSetAccepted();
    }
}
