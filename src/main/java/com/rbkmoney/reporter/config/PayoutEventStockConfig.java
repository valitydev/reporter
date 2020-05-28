package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.reporter.listener.stockevent.StockEventHandlerClientImpl;
import com.rbkmoney.sink.common.handle.stockevent.StockEventHandler;
import com.rbkmoney.sink.common.handle.stockevent.event.PayoutEventHandler;
import com.rbkmoney.sink.common.handle.stockevent.event.change.PayoutChangeEventHandler;
import com.rbkmoney.sink.common.handle.stockevent.event.impl.PayoutChangePayoutStockEventHandler;
import com.rbkmoney.sink.common.handle.stockevent.impl.PayoutEventStockEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PayoutEventStockConfig {

    @Bean
    public PayoutEventHandler payoutChangePayoutStockEventHandler(List<PayoutChangeEventHandler> eventHandlers) {
        return new PayoutChangePayoutStockEventHandler(eventHandlers);
    }

    @Bean
    public StockEventHandler<StockEvent> payoutEventStockEventHandler(List<PayoutEventHandler> eventHandlers) {
        return new PayoutEventStockEventHandler(eventHandlers);
    }

    @Bean
    public EventHandler<StockEvent> payoutStockEventHandler(StockEventHandler<StockEvent> payoutEventStockEventHandler) {
        return new StockEventHandlerClientImpl(payoutEventStockEventHandler);
    }
}
