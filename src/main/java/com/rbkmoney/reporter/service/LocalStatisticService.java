package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.reporter.domain.tables.records.InvoiceRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import org.jooq.Cursor;

import java.time.LocalDateTime;
import java.util.Map;

public interface LocalStatisticService {

    ShopAccountingModel getShopAccounting(String partyId, String shopId, String currencyCode, LocalDateTime toTime);

    ShopAccountingModel getShopAccounting(String partyId,
                                          String shopId,
                                          String currencyCode,
                                          LocalDateTime fromTime,
                                          LocalDateTime toTime);

    Map<String, String> getPurposes(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime);

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
