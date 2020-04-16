package com.rbkmoney.reporter.handler.partymanagement;

import com.rbkmoney.machinegun.eventsink.MachineEvent;

public interface EventHandler<T> {

    void handle(MachineEvent machineEvent, T value);

}
