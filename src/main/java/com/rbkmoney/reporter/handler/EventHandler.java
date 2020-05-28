package com.rbkmoney.reporter.handler;

import com.rbkmoney.machinegun.eventsink.MachineEvent;

public interface EventHandler<T> {

    void handle(MachineEvent machineEvent, T value, int changeId) throws Exception;

    boolean isAccept(T value);

}
