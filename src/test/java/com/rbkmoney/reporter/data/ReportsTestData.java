package com.rbkmoney.reporter.data;

import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.enums.PaymentFlow;
import com.rbkmoney.reporter.domain.enums.PaymentPayerType;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ReportsTestData {

    public static final String DEFAULT_CURRENCY = "RUB";

    public static AdjustmentRecord buildAdjustmentRecord(int i,
                                                         String partyId,
                                                         String shopId,
                                                         Long amount,
                                                         LocalDateTime createdAt) {
        AdjustmentRecord adjustment = new AdjustmentRecord();
        adjustment.setAdjustmentId("id" + i);
        adjustment.setPaymentId("paymentId" + i);
        adjustment.setInvoiceId("invoiceId" + i);
        adjustment.setAmount(amount);
        adjustment.setShopId(shopId);
        adjustment.setPartyId(partyId);
        adjustment.setCurrencyCode(DEFAULT_CURRENCY);
        adjustment.setStatus(AdjustmentStatus.captured);
        adjustment.setCreatedAt(createdAt);
        adjustment.setStatusCreatedAt(createdAt);
        adjustment.setReason("You are the reason of my life");
        return adjustment;
    }

    public static RefundRecord buildRefundRecord(int i,
                                                 String partyId,
                                                 String shopId,
                                                 long amount,
                                                 LocalDateTime createdAt) {
        RefundRecord refund = new RefundRecord();
        refund.setPaymentId("paymentId" + i);
        refund.setInvoiceId("invoiceId" + i);
        refund.setRefundId("" + i);
        refund.setPartyId(partyId);
        refund.setShopId(shopId);
        refund.setStatus(RefundStatus.succeeded);
        refund.setCreatedAt(createdAt);
        refund.setStatusCreatedAt(createdAt);
        refund.setAmount(amount);
        refund.setCurrencyCode(DEFAULT_CURRENCY);
        refund.setReason("You are the reason of my life");
        return refund;
    }

    public static PaymentRecord buildPaymentRecord(int index,
                                                   String partyId,
                                                   String shopId,
                                                   LocalDateTime createdAt) {
        long amount = 123L + index;
        long feeAmount = 2L + index;
        return buildPaymentRecord(index, partyId, shopId, amount, feeAmount, createdAt);
    }

    public static PaymentRecord buildPaymentRecord(int index) {
        long amount = 123L + index;
        long feeAmount = 2L + index;
        return buildPaymentRecord(index, "party", "shop", amount, feeAmount, LocalDateTime.now());
    }

    public static PaymentRecord buildPaymentRecord(int index, String partyId, String shopId) {
        long amount = 123L + index;
        long feeAmount = 2L + index;
        return buildPaymentRecord(index, partyId, shopId, amount, feeAmount, LocalDateTime.now());
    }

    public static PaymentRecord buildPaymentRecord(int index,
                                                   String partyId,
                                                   String shopId,
                                                   Long amount,
                                                   Long feeAmount,
                                                   LocalDateTime statusCreatedAt) {
        PaymentRecord payment = new PaymentRecord();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setInvoiceId("invoiceId" + index);
        payment.setPaymentId("paymentId" + index);
        payment.setPartyId(partyId);
        payment.setShopId(shopId);
        payment.setTool(com.rbkmoney.reporter.domain.enums.PaymentTool.bank_card);
        payment.setFlow(PaymentFlow.instant);
        payment.setStatus(com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus.captured);
        payment.setStatusCreatedAt(statusCreatedAt);
        payment.setPayerType(PaymentPayerType.payment_resource);
        payment.setEmail("abc" + index + "@mail.ru");
        payment.setAmount(amount);
        payment.setFee(feeAmount);
        payment.setCurrencyCode(DEFAULT_CURRENCY);
        return payment;
    }

}
