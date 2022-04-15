package dev.vality.reporter.service;

import dev.vality.damsel.payment_processing.Invoice;

public interface HellgateInvoicingService {

    Invoice getInvoice(String invoiceId, long sequenceId) throws Exception;

}
