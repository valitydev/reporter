package dev.vality.reporter.service.impl;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.handler.EventHandler;
import dev.vality.reporter.model.KafkaEvent;
import dev.vality.reporter.service.EventService;
import dev.vality.sink.common.parser.Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicingService implements EventService {

    private final Parser<MachineEvent, EventPayload> paymentMachineEventParser;

    private final List<EventHandler<InvoiceChange>> payloadHandlers;

    @Value("${kafka.topics.invoicing.throttling-timeout-ms}")
    private int throttlingTimeout;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleEvents(List<KafkaEvent> kafkaEvents) throws Exception {
        for (KafkaEvent kafkaEvent : kafkaEvents) {
            MachineEvent machineEvent = kafkaEvent.getEvent();
            EventPayload eventPayload = paymentMachineEventParser.parse(machineEvent);
            if (eventPayload.isSetInvoiceChanges()) {
                processInvoiceChanges(kafkaEvent, eventPayload.getInvoiceChanges());
            }
        }
    }

    private void processInvoiceChanges(KafkaEvent kafkaEvent,
                                       List<InvoiceChange> invoiceChanges) throws Exception {
        for (int i = 0; i < invoiceChanges.size(); i++) {
            InvoiceChange change = invoiceChanges.get(i);
            for (EventHandler<InvoiceChange> handler : payloadHandlers) {
                if (handler.isAccept(change)) {
                    handler.handle(kafkaEvent, change, i);
                    Thread.sleep(throttlingTimeout);
                }
            }
        }
    }

}
