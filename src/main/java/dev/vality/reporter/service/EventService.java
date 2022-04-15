package dev.vality.reporter.service;

import dev.vality.reporter.model.KafkaEvent;

import java.util.List;

public interface EventService {

    void handleEvents(List<KafkaEvent> machineEvents) throws Exception;

}
