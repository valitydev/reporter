package dev.vality.reporter.handler;

import dev.vality.reporter.model.KafkaEvent;

public interface EventHandler<T> {

    void handle(KafkaEvent kafkaEvent, T value, int changeId) throws Exception;

    boolean isAccept(T value);

}
