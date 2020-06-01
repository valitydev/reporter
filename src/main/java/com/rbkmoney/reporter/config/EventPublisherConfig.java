package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.eventstock.client.poll.PollingEventPublisherBuilder;
import com.rbkmoney.reporter.config.properties.PayoutProperties;
import com.rbkmoney.reporter.listener.stockevent.PayoutOnStart;
import com.rbkmoney.reporter.service.PayoutEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class EventPublisherConfig {

    @Bean
    public EventPublisher payoutEventPublisher(EventHandler<Event> payoutEventStockHandler,
                                               PayoutProperties properties) throws IOException {
        return new PollingEventPublisherBuilder()
                .withURI(properties.getUrl().getURI())
                .withPayoutServiceAdapter()
                .withHousekeeperTimeout(properties.getHousekeeperTimeout())
                .withEventHandler(payoutEventStockHandler)
                .withMaxPoolSize(properties.getMaxPoolSize())
                .withMaxQuerySize(properties.getMaxQuerySize())
                .withPollDelay(properties.getDelay())
                .withEventRetryDelay(properties.getRetryDelay())
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "payouter.polling.enabled", havingValue = "true")
    public ApplicationListener<ApplicationReadyEvent> payoutOnStart(EventPublisher payoutEventPublisher,
                                                                    PayoutEventService eventService) {
        return new PayoutOnStart(payoutEventPublisher, eventService);
    }
}
