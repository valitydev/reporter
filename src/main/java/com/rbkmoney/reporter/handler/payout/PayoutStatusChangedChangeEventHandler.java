package com.rbkmoney.reporter.handler.payout;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.PayoutChange;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.dao.PayoutDao;
import com.rbkmoney.reporter.domain.enums.PayoutStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
import com.rbkmoney.reporter.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PayoutStatusChangedChangeEventHandler extends AbstractPayoutHandler {

    private final PayoutDao payoutDao;

    @Override
    public void handle(PayoutChange payload, Event event) {
        String payoutId = event.getSource().getPayoutId();

        log.info("Start payout status changed handling, payoutId={}", payoutId);

        Payout payout = payoutDao.getPayout(payoutId);
        if (payout == null) {
            throw new NotFoundException(String.format("Source payout with id %s not found!", payoutId));
        }

        PayoutState payoutState = new PayoutState();
        payoutState.setEventId(event.getId());
        payoutState.setExtPayoutId(payout.getId());
        payoutState.setEventCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        payoutState.setPayoutId(payoutId);
        var damselPayoutStatus = payload.getPayoutStatusChanged().getStatus();
        payoutState.setStatus(TBaseUtil.unionFieldToEnum(damselPayoutStatus, PayoutStatus.class));
        if (damselPayoutStatus.isSetCancelled()) {
            payoutState.setCancelDetails(damselPayoutStatus.getCancelled().getDetails());
        }

        payoutDao.savePayoutState(payoutState);
        log.info("Payout status has been changed, payoutId={}", payoutId);
    }

    @Override
    public boolean accept(PayoutChange payload) {
        return payload.isSetPayoutStatusChanged();
    }
}
