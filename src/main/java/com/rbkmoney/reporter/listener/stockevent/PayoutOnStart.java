package com.rbkmoney.reporter.listener.stockevent;

import com.rbkmoney.eventstock.client.DefaultSubscriberConfig;
import com.rbkmoney.eventstock.client.EventConstraint;
import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.eventstock.client.SubscriberConfig;
import com.rbkmoney.eventstock.client.poll.EventFlowFilter;
import com.rbkmoney.reporter.service.PayoutEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.Optional;

@RequiredArgsConstructor
public class PayoutOnStart implements ApplicationListener<ApplicationReadyEvent> {

    private final EventPublisher payoutEventPublisher;

    private final PayoutEventService eventService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        payoutEventPublisher.subscribe(buildSubscriberConfig(eventService.getPayoutLastEventId()));
    }

    private SubscriberConfig buildSubscriberConfig(Optional<Long> lastEventIdOptional) {
        EventConstraint.EventIDRange eventIDRange = new EventConstraint.EventIDRange();
        lastEventIdOptional.ifPresent(eventIDRange::setFromExclusive);
        EventFlowFilter eventFlowFilter = new EventFlowFilter(new EventConstraint(eventIDRange));
        return new DefaultSubscriberConfig(eventFlowFilter);
    }
}
