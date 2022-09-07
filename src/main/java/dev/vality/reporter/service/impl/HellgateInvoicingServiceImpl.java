package dev.vality.reporter.service.impl;

import dev.vality.damsel.payment_processing.EventRange;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.reporter.service.HellgateInvoicingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HellgateInvoicingServiceImpl implements HellgateInvoicingService {

    private final InvoicingSrv.Iface hgInvoicingService;

    @Override
    public Invoice getInvoice(String invoiceId, long sequenceId) throws Exception {
        return hgInvoicingService.get(invoiceId, getEventRange((int) sequenceId));
    }

    private EventRange getEventRange(int sequenceId) {
        return new EventRange().setLimit(sequenceId);
    }

}
