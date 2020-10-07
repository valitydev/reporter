package com.rbkmoney.reporter.handler;

import com.rbkmoney.reporter.model.KafkaEvent;

public interface EventHandler<T> {

    void handle(KafkaEvent kafkaEvent, T value, int changeId) throws Exception;

    boolean isAccept(T value);

}
