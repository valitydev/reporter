package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.domain.InvoicePaymentAdjustment;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentRefund;
import com.rbkmoney.reporter.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InvoicingServiceUtils {

    public static InvoicePayment getInvoicePaymentById(Invoice invoice,
                                                       String paymentId,
                                                       String invoiceId,
                                                       Long sequenceId,
                                                       int changeId) {
        return invoice.getPayments().stream()
                .filter(invPayment -> invPayment.isSetPayment()
                        && paymentId.equals(invPayment.getPayment().getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Payment for invoice (invoice id '%s', " +
                        "sequenceId = '%d' and changeId = '%d') not found!", invoiceId, sequenceId, changeId)));
    }

    public static InvoicePaymentAdjustment getInvoicePaymentAdjustmentById(InvoicePayment invoicePayment,
                                                                           String adjustmentId,
                                                                           String invoiceId,
                                                                           Long sequenceId,
                                                                           int changeId) {
        if (!invoicePayment.isSetAdjustments()) {
            throw new NotFoundException(String.format("Adjustments for invoice not found! (invoice id '%s', " +
                    "sequenceId = '%d' and changeId = '%d')", invoiceId, sequenceId, changeId));
        }
        return invoicePayment.getAdjustments().stream()
                .filter(hgAdj -> adjustmentId.equals(hgAdj.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("InvoicePaymentAdjustment for payment " +
                        "(invoice id '%s', sequence id '%d', change id '%d') not found!", invoiceId, sequenceId, changeId)));
    }

    public static InvoicePaymentRefund getInvoicePaymentRefundById(InvoicePayment invoicePayment,
                                                                   String refundId,
                                                                   String invoiceId,
                                                                   Long sequenceId,
                                                                   int changeId) {
        if (!invoicePayment.isSetRefunds()) {
            throw new NotFoundException(String.format("Refunds for invoice not found! (invoice id '%s', " +
                    "sequenceId = '%d' and changeId = '%d')", invoiceId, sequenceId, changeId));
        }
        return invoicePayment.getRefunds().stream()
                .filter(hgRefund -> refundId.equals(hgRefund.getRefund().getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("InvoicePaymentRefund or sessions for refund " +
                        "(invoice id '%s', sequence id '%d', change id '%d') not found!",
                        invoiceId, sequenceId, changeId)));
    }

}
