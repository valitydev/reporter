package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.EventPayload;
import com.rbkmoney.reporter.dao.PayoutDao;
import com.rbkmoney.reporter.handler.payout.AbstractPayoutHandler;
import com.rbkmoney.reporter.service.PayoutEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutEventServiceImpl implements PayoutEventService {

    private final PayoutDao payoutDao;

    private final List<AbstractPayoutHandler> payoutHandlers;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleEvents(Event payoutEvent, EventPayload payload) {
        payload.getPayoutChanges().forEach(change -> payoutHandlers.forEach(payoutHandler -> {
            if (payoutHandler.accept(change)) {
                payoutHandler.handle(change, payoutEvent);
            }
        }));
    }

    @Override
    public Optional<Long> getLastEventId() {
        Optional<Long> lastEventId = payoutDao.getLastEventId();
        log.info("Last payout eventId={}", lastEventId);
        return lastEventId;
    }
}
