package com.rbkmoney.reporter.config;

import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.eventstock.client.poll.PollingEventPublisherBuilder;
import com.rbkmoney.reporter.handler.EventStockHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class EventStockConfig {

    @Bean
    public EventPublisher eventPublisher(
            EventStockHandler eventStockHandler,
            @Value("${bustermaze.url}") Resource resource,
            @Value("${bustermaze.polling.delay}") int pollDelay,
            @Value("${bustermaze.polling.maxPoolSize}") int maxPoolSize
    ) throws IOException {
        return new PollingEventPublisherBuilder()
                .withURI(resource.getURI())
                .withEventHandler(eventStockHandler)
                .withMaxPoolSize(maxPoolSize)
                .withEventRetryDelay(pollDelay)
                .withPollDelay(pollDelay)
                .build();
    }

}