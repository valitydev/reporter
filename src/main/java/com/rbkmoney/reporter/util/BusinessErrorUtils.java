package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.reporter.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessErrorUtils {

    public static void checkInvoiceCorrectness(Invoice invoice, String invoiceId, Long sequenceId, int changeId) {
        if (invoice == null || !invoice.isSetPayments()) {
            throw new NotFoundException(String.format("Invoice or payments not found! (invoice id '%s', " +
                    "sequenceId = '%d' and changeId = '%d')", invoiceId, sequenceId, changeId));
        }
    }

}
