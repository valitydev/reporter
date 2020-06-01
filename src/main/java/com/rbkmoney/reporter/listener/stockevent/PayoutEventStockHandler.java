package com.rbkmoney.reporter.listener.stockevent;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.reporter.service.impl.PayoutEventServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutEventStockHandler implements EventHandler<Event> {

    private final PayoutEventServiceImpl payoutService;

    @Override
    public EventAction handle(Event event, String subsKey) {
        try {
            payoutService.handleEvents(event, event.getPayload());
        } catch (RuntimeException e) {
            log.error("Error when polling payout event with id={}", event.getId(), e);
            return EventAction.DELAYED_RETRY;
        }
        return EventAction.CONTINUE;
    }

}
