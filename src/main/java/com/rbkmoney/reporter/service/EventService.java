package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.model.KafkaEvent;

import java.util.List;

public interface EventService {

    void handleEvents(List<KafkaEvent> machineEvents) throws Exception;

}
