package com.rbkmoney.reporter.handler.payout;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.PayoutChange;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.dao.PayoutDao;
import com.rbkmoney.reporter.domain.enums.PayoutStatus;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
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
        var damselPayoutStatus = payload.getPayoutStatusChanged().getStatus();
        String payoutId = event.getSource().getPayoutId();

        log.info("Start payout status changed handling, payoutId={}", payoutId);

        PayoutState payoutState = new PayoutState();
        payoutState.setEventId(event.getId());
        payoutState.setEventCreatedAt(TypeUtil.stringToLocalDateTime(event.getCreatedAt()));
        payoutState.setPayoutId(payoutId);
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
