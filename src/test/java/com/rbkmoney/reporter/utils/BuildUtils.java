package com.rbkmoney.reporter.utils;

import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.geck.common.util.TypeUtil;

import java.time.LocalDateTime;

public class BuildUtils {

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


    public static StatPayment buildStatPayment(int i) {
        StatPayment payment = new StatPayment();
        payment.setId("id" + i);
        payment.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        payment.setInvoiceId("invoiceId" + i);
        InvoicePaymentCaptured invoicePaymentCaptured = new InvoicePaymentCaptured();
        invoicePaymentCaptured.setAt("201" + i + "-03-22T06:12:27Z");
        payment.setStatus(InvoicePaymentStatus.captured(invoicePaymentCaptured));
        PaymentResourcePayer paymentResourcePayer = new PaymentResourcePayer(PaymentTool.bank_card(new BankCard("token", null, "424" + i, "56789" + i)));
        paymentResourcePayer.setEmail("abc" + i + "@mail.ru");
        payment.setPayer(Payer.payment_resource(paymentResourcePayer));
        payment.setAmount(123L + i);
        payment.setFee(2L + i);
        payment.setShopId("shopId" + i);
        payment.setCurrencySymbolicCode("RUB");
        return payment;
    }
}
