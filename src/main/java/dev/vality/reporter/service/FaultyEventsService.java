package dev.vality.reporter.service;

import dev.vality.reporter.model.KafkaEvent;

public interface FaultyEventsService {

    boolean isFaultyEvent(KafkaEvent event);

}
