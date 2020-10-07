package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.model.KafkaEvent;

public interface FaultyEventsService {

    boolean isFaultyEvent(KafkaEvent event);

}
