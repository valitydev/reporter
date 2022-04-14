package dev.vality.reporter.service;

import dev.vality.reporter.domain.tables.records.AdjustmentRecord;
import dev.vality.reporter.domain.tables.records.InvoiceRecord;
import dev.vality.reporter.domain.tables.records.PaymentRecord;
import dev.vality.reporter.domain.tables.records.RefundRecord;
import org.jooq.Cursor;

import java.time.LocalDateTime;

public interface LocalStatisticService {

    String getPurpose(String invoiceId);

    InvoiceRecord getInvoice(String invoiceId);

    Cursor<PaymentRecord> getPaymentsCursor(String partyId,
                                            String shopId,
                                            LocalDateTime fromTime,
                                            LocalDateTime toTime);

    PaymentRecord getCapturedPayment(String partyId, String shopId, String invoiceId, String paymentId);

    Cursor<RefundRecord> getRefundsCursor(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime);

    Cursor<AdjustmentRecord> getAdjustmentCursor(String partyId,
                                                 String shopId,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime);

}
