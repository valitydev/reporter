package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.enums.PaymentFlow;
import com.rbkmoney.reporter.domain.enums.PaymentPayerType;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.rbkmoney.reporter.model.StatAdjustment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class BuildUtils {

    public static StatAdjustment buildStatAdjustment(int i, String shopId) {
        StatAdjustment statAdjustment = new StatAdjustment();
        statAdjustment.setAdjustmentId("id" + i);
        statAdjustment.setPaymentId("paymentId" + i);
        statAdjustment.setInvoiceId("invoiceId" + i);
        statAdjustment.setAdjustmentAmount(123L + i);
        statAdjustment.setPartyShopId(shopId);
        statAdjustment.setAdjustmentCurrencyCode("RUB");
        statAdjustment.setAdjustmentStatusCreatedAt(Instant.parse("2020-10-22T06:12:27Z"));
        statAdjustment.setAdjustmentReason("You are the reason of my life");
        return statAdjustment;
    }

    public static StatAdjustment buildStatAdjustment(int i) {
        return buildStatAdjustment(i, "shopId" + i);
    }

    public static AdjustmentRecord buildStatAdjustmentRecord(int i, String partyId, String shopId) {
        AdjustmentRecord adjustment = new AdjustmentRecord();
        adjustment.setAdjustmentId("id" + i);
        adjustment.setPaymentId("paymentId" + i);
        adjustment.setInvoiceId("invoiceId" + i);
        adjustment.setAmount(123L + i);
        adjustment.setShopId(shopId);
        adjustment.setPartyId(partyId);
        adjustment.setCurrencyCode("RUB");
        adjustment.setStatus(AdjustmentStatus.captured);
        adjustment.setCreatedAt(Instant.parse("2020-10-22T06:12:27Z")
                .atZone(ZoneOffset.UTC).toLocalDateTime());
        adjustment.setReason("You are the reason of my life");
        return adjustment;
    }

    public static StatRefund buildStatRefund(int i) {
        StatRefund refund = new StatRefund();
        refund.setId("id" + i);
        refund.setPaymentId("paymentId" + i);
        refund.setInvoiceId("invoiceId" + i);
        refund.setStatus(InvoicePaymentRefundStatus.succeeded(new InvoicePaymentRefundSucceeded("201" + i + "-03-22T06:12:27Z")));
        refund.setAmount(123L + i);
        refund.setShopId("shopId" + i);
        refund.setId("" + i);
        refund.setCurrencySymbolicCode("RUB");
        refund.setReason("You are the reason of my life");
        return refund;
    }

    public static StatRefund buildStatRefund(int i, String shopId) {
        StatRefund refund = buildStatRefund(i);
        refund.setShopId(shopId);
        return refund;
    }

    public static RefundRecord buildRefundRecord(int i, String partyId, String shopId) {
        RefundRecord refund = new RefundRecord();
        refund.setPaymentId("paymentId" + i);
        refund.setInvoiceId("invoiceId" + i);
        refund.setRefundId("" + i);
        refund.setPartyId(partyId);
        refund.setShopId(shopId);
        refund.setStatus(RefundStatus.succeeded);
        LocalDateTime localDateTime = Instant.parse("201" + i + "-03-22T06:12:27Z")
                .atZone(ZoneOffset.UTC).toLocalDateTime();
        refund.setCreatedAt(localDateTime);
        refund.setStatusCreatedAt(localDateTime);
        refund.setAmount(123L + i);
        refund.setCurrencyCode("RUB");
        refund.setReason("You are the reason of my life");
        return refund;
    }

    public static StatPayment buildStatPayment(int i) {
        StatPayment payment = new StatPayment();
        payment.setId("id" + i);
        payment.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        payment.setInvoiceId("invoiceId" + i);
        InvoicePaymentCaptured invoicePaymentCaptured = new InvoicePaymentCaptured();
        invoicePaymentCaptured.setAt("201" + i + "-03-22T06:12:27Z");
        payment.setStatus(InvoicePaymentStatus.captured(invoicePaymentCaptured));
        PaymentResourcePayer paymentResourcePayer = new PaymentResourcePayer(PaymentTool.bank_card(
                new BankCard("token", null, "424" + i, "56789" + i))
        );
        paymentResourcePayer.setEmail("abc" + i + "@mail.ru");
        payment.setPayer(Payer.payment_resource(paymentResourcePayer));
        payment.setAmount(123L + i);
        payment.setFee(2L + i);
        payment.setShopId("shopId" + i);
        payment.setCurrencySymbolicCode("RUB");
        return payment;
    }

    public static StatPayment buildStatPayment(int i, String shopId) {
        StatPayment payment = buildStatPayment(i);
        payment.setId("paymentId" + i);
        payment.setShopId(shopId);
        return payment;
    }

    public static PaymentRecord buildPaymentRecord(int i, String partyId, String shopId) {
        PaymentRecord payment = new PaymentRecord();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setInvoiceId("invoiceId" + i);
        payment.setPaymentId("paymentId" + i);
        payment.setPartyId(partyId);
        payment.setShopId(shopId);
        payment.setTool(com.rbkmoney.reporter.domain.enums.PaymentTool.bank_card);
        payment.setFlow(PaymentFlow.instant);
        payment.setStatus(com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus.captured);
        payment.setStatusCreatedAt(Instant.parse("201" + i + "-03-22T06:12:27Z").atZone(ZoneOffset.UTC).toLocalDateTime());
        payment.setPayerType(PaymentPayerType.payment_resource);
        payment.setEmail("abc" + i + "@mail.ru");
        payment.setAmount(123L + i);
        payment.setFee(2L + i);
        payment.setCurrencyCode("RUB");
        return payment;
    }

    public static Map<String, String> buildPurposes(int count) {
        Map<String, String> purposes = new HashMap<>();
        for (int i = 0; i < count; i++) {
            purposes.put("invoiceId" + i, "product" + i);
        }
        return purposes;
    }

    public static StatPayment buildStatPayment() {
        StatPayment payment = new StatPayment();
        InvoicePaymentCaptured invoicePaymentCaptured = new InvoicePaymentCaptured();
        invoicePaymentCaptured.setAt("2018-03-22T06:12:27Z");
        payment.setStatus(InvoicePaymentStatus.captured(invoicePaymentCaptured));
        PaymentResourcePayer paymentResourcePayer = new PaymentResourcePayer(PaymentTool.bank_card(new BankCard("token", null, "4249", "567890")));
        paymentResourcePayer.setEmail("xyz@mail.ru");
        payment.setPayer(Payer.payment_resource(paymentResourcePayer));
        return payment;
    }
}
