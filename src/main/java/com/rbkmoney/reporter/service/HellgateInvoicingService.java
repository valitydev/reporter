package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.payment_processing.Invoice;

public interface HellgateInvoicingService {

    Invoice getInvoice(String invoiceId, long sequenceId) throws Exception;

}
