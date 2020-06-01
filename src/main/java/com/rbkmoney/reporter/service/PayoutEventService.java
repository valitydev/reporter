package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.EventPayload;

import java.util.Optional;

public interface PayoutEventService {

    void handleEvents(Event processingEvent, EventPayload payload);

    Optional<Long> getLastEventId();

}
