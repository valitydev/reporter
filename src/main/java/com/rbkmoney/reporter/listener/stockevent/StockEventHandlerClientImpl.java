package com.rbkmoney.reporter.listener.stockevent;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.sink.common.handle.stockevent.StockEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StockEventHandlerClientImpl implements EventHandler<StockEvent> {

    private final StockEventHandler<StockEvent> eventHandler;

    @Override
    public EventAction handle(StockEvent stockEvent, String s) throws Exception {
        try {
            if (eventHandler.accept(stockEvent)) {
                eventHandler.handle(stockEvent, stockEvent);
            }
            return EventAction.CONTINUE;
        } catch (Exception ex) {
            log.warn("Failed to handle event, retry", ex);
            return EventAction.DELAYED_RETRY;
        }
    }
}
