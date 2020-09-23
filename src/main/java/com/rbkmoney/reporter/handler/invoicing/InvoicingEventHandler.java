package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import org.jooq.Query;

import java.util.List;

public interface InvoicingEventHandler {

    List<Query> handle(MachineEvent machineEvent, InvoiceChange value, int changeId) throws Exception;

    boolean isAccept(InvoiceChange value);

}
