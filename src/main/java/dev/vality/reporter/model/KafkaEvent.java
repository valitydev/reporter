package dev.vality.reporter.model;

import dev.vality.machinegun.eventsink.MachineEvent;
import lombok.Data;

@Data
public class KafkaEvent {

    private final String topic;
    private final int partition;
    private final long offset;
    private final MachineEvent event;

}
