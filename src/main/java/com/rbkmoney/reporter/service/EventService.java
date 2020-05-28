package com.rbkmoney.reporter.service;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.handler.EventHandler;

import java.util.List;

public interface EventService<T> {

    void handleEvents(List<MachineEvent> machineEvents) throws Exception;

    default void handleIfAccept(List<EventHandler<T>> handlers,
                                MachineEvent machineEvent,
                                T change,
                                int changeId) throws Exception {
        for (EventHandler<T> handler : handlers) {
            if (handler.isAccept(change)) {
                handler.handle(machineEvent, change, changeId);
            }
        }
    }
}
