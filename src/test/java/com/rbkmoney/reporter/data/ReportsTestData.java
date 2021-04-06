package com.rbkmoney.reporter.data;

import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.damsel.merch_stat.InvoicePaymentStatus;
import com.rbkmoney.damsel.merch_stat.PaymentTool;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.enums.*;
import com.rbkmoney.reporter.domain.enums.PayoutStatus;
import com.rbkmoney.reporter.domain.enums.PayoutType;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
import com.rbkmoney.reporter.domain.tables.records.*;
import com.rbkmoney.reporter.model.StatAdjustment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class ReportsTestData {

    public static final String DEFAULT_CURRENCY = "RUB";

    public static StatAdjustment buildStatAdjustment(int i, String shopId) {
        StatAdjustment statAdjustment = new StatAdjustment();
        statAdjustment.setAdjustmentId("id" + i);
        statAdjustment.setPaymentId("paymentId" + i);
        statAdjustment.setInvoiceId("invoiceId" + i);
        statAdjustment.setAdjustmentAmount(123L + i);
        statAdjustment.setPartyShopId(shopId);
        statAdjustment.setAdjustmentCurrencyCode(DEFAULT_CURRENCY);
        statAdjustment.setAdjustmentStatusCreatedAt(Instant.parse("2020-10-22T06:12:27Z"));
        statAdjustment.setAdjustmentReason("You are the reason of my life");
        return statAdjustment;
    }

    public static StatAdjustment buildStatAdjustment(int i) {
        return buildStatAdjustment(i, "shopId" + i);
    }

    public static AdjustmentRecord buildStatAdjustmentRecord(int i,
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

    public static StatRefund buildStatRefund(int i) {
        StatRefund refund = new StatRefund();
        refund.setId("id" + i);
        refund.setPaymentId("paymentId" + i);
        refund.setInvoiceId("invoiceId" + i);
        refund.setStatus(InvoicePaymentRefundStatus
                .succeeded(new InvoicePaymentRefundSucceeded("201" + i + "-03-22T06:12:27Z")));
        refund.setAmount(123L + i);
        refund.setShopId("shopId" + i);
        refund.setId("" + i);
        refund.setCurrencySymbolicCode(DEFAULT_CURRENCY);
        refund.setReason("You are the reason of my life");
        return refund;
    }

    public static StatRefund buildStatRefund(int i, String shopId) {
        StatRefund refund = buildStatRefund(i);
        refund.setShopId(shopId);
        return refund;
    }

    public static RefundRecord buildRefundRecord(int i,
                                                 String partyId,
                                                 String shopId,
                                                 long amount,
                                                 LocalDateTime ceratedAt) {
        RefundRecord refund = new RefundRecord();
        refund.setPaymentId("paymentId" + i);
        refund.setInvoiceId("invoiceId" + i);
        refund.setRefundId("" + i);
        refund.setPartyId(partyId);
        refund.setShopId(shopId);
        refund.setStatus(RefundStatus.succeeded);
        refund.setCreatedAt(ceratedAt);
        refund.setStatusCreatedAt(ceratedAt);
        refund.setAmount(amount);
        refund.setCurrencyCode(DEFAULT_CURRENCY);
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
        payment.setCurrencySymbolicCode(DEFAULT_CURRENCY);
        return payment;
    }

    public static StatPayment buildStatPayment(int i, String shopId) {
        StatPayment payment = buildStatPayment(i);
        payment.setId("paymentId" + i);
        payment.setShopId(shopId);
        return payment;
    }

    public static StatPayment buildStatPayment() {
        StatPayment payment = new StatPayment();
        InvoicePaymentCaptured invoicePaymentCaptured = new InvoicePaymentCaptured();
        invoicePaymentCaptured.setAt("2018-03-22T06:12:27Z");
        payment.setStatus(InvoicePaymentStatus.captured(invoicePaymentCaptured));
        PaymentResourcePayer paymentResourcePayer =
                new PaymentResourcePayer(PaymentTool.bank_card(new BankCard("token", null, "4249", "567890")));
        paymentResourcePayer.setEmail("xyz@mail.ru");
        payment.setPayer(Payer.payment_resource(paymentResourcePayer));
        return payment;
    }

    public static PaymentRecord buildPaymentRecord(int index, String partyId, String shopId) {
        LocalDateTime dateTime = Instant.parse("201" + index + "-03-22T06:12:27Z")
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();
        long amount = 123L + index;
        long feeAmount = 2L + index;
        return buildPaymentRecord(index, partyId, shopId, amount, feeAmount, dateTime);
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

    public static Map<String, String> buildPurposes(int count) {
        Map<String, String> purposes = new HashMap<>();
        for (int i = 0; i < count; i++) {
            purposes.put("invoiceId" + i, "product" + i);
        }
        return purposes;
    }

    public static PayoutRecord buildPayoutRecord(int i,
                                                 String partyId,
                                                 String shopId,
                                                 Long amount,
                                                 LocalDateTime createdAt) {
        PayoutRecord record = new PayoutRecord();
        record.setPartyId(partyId);
        record.setShopId(shopId);
        record.setPayoutId("payout." + i);
        record.setContractId("contract-1");
        record.setCreatedAt(createdAt);
        record.setAmount(amount);
        record.setFee(1L);
        record.setCurrencyCode(DEFAULT_CURRENCY);
        record.setWalletId("wallet.1");
        record.setSummary("Some summary");
        record.setType(PayoutType.bank_card);
        return record;
    }

    public static PayoutStateRecord buildPayoutStateRecord(int i, LocalDateTime createdAt) {
        PayoutStateRecord record = new PayoutStateRecord();
        record.setEventId(Long.valueOf(i));
        record.setEventCreatedAt(createdAt);
        record.setPayoutId("payout." + i);
        record.setExtPayoutId(Long.valueOf(i));
        record.setStatus(PayoutStatus.paid);
        record.setCancelDetails("null");
        return record;
    }

    public static ChargebackRecord buildChargebackRecord(int i,
                                                         String partyId,
                                                         String shopId,
                                                         Long amount,
                                                         LocalDateTime createdAt) {
        ChargebackRecord record = new ChargebackRecord();
        record.setDomainRevision(1L);
        record.setPartyRevision(1L);
        record.setInvoiceId("invoice." + i);
        record.setPaymentId("payment." + i);
        record.setChargebackId("chargeback." + i);
        record.setShopId(shopId);
        record.setPartyId(partyId);
        record.setExternalId("extId." + i);
        record.setEventCreatedAt(createdAt);
        record.setCreatedAt(createdAt);
        record.setStatus(ChargebackStatus.accepted);
        record.setLevyAmount(amount);
        record.setLevyCurrencyCode(DEFAULT_CURRENCY);
        record.setAmount(amount);
        record.setCurrencyCode(DEFAULT_CURRENCY);
        record.setReasonCode("1");
        record.setReasonCategory(ChargebackCategory.fraud);
        record.setStage(ChargebackStage.arbitration);
        return record;
    }
}
