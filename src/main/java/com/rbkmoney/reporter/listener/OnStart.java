package com.rbkmoney.reporter.listener;

import com.rbkmoney.eventstock.client.DefaultSubscriberConfig;
import com.rbkmoney.eventstock.client.EventConstraint;
import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.eventstock.client.SubscriberConfig;
import com.rbkmoney.eventstock.client.poll.EventFlowFilter;
import com.rbkmoney.reporter.dao.ContractMetaDao;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.exception.StorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OnStart implements ApplicationListener<ApplicationReadyEvent> {

    private final EventPublisher eventPublisher;

    private final ContractMetaDao contractMetaDao;

    @Autowired
    public OnStart(EventPublisher eventPublisher, ContractMetaDao contractMetaDao) {
        this.eventPublisher = eventPublisher;
        this.contractMetaDao = contractMetaDao;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        EventConstraint.EventIDRange eventIDRange = new EventConstraint.EventIDRange();

        try {
            Optional.ofNullable(contractMetaDao.getLastEventId())
                    .ifPresent(eventId -> eventIDRange.setFromExclusive(eventId));
        } catch (DaoException ex) {
            throw new StorageException("failed to get last event id from storage", ex);
        }

        SubscriberConfig subscriberConfig = new DefaultSubscriberConfig<>(
                new EventFlowFilter(
                        new EventConstraint(eventIDRange)
                )
        );

        eventPublisher.subscribe(subscriberConfig);
    }
}
