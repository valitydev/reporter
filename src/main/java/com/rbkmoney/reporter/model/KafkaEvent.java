package com.rbkmoney.reporter.model;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.Data;

@Data
public class KafkaEvent {

    private final String topic;
    private final int partition;
    private final long offset;
    private final MachineEvent event;

}
