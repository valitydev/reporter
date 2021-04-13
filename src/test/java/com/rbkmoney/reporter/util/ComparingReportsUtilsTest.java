package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.model.StatAdjustment;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComparingReportsUtilsTest {

    private static final String INVOICE_ID = "inv";
    private static final String PAYMENT_ID = "1";
    private static final String REFUND_ID = "2";
    private static final String ADJUSTMENT_ID = "3";

    @Test
    public void isEqualPaymentsTest() {

        StatPayment statPayment = new StatPayment();
        statPayment.setInvoiceId(INVOICE_ID);
        statPayment.setId(PAYMENT_ID);

        Payment payment = new Payment();
        payment.setInvoiceId(INVOICE_ID);
        payment.setPaymentId(PAYMENT_ID);

        assertTrue(ComparingReportsUtils.isEqualPayments(statPayment, payment));

        payment.setPaymentId(PAYMENT_ID + PAYMENT_ID);
        assertFalse(ComparingReportsUtils.isEqualPayments(statPayment, payment));
    }

    @Test
    public void isEqualRefundsTest() {
        StatRefund statRefund = new StatRefund();
        statRefund.setInvoiceId(INVOICE_ID);
        statRefund.setPaymentId(PAYMENT_ID);
        statRefund.setId(REFUND_ID);

        Refund refund = new Refund();
        refund.setInvoiceId(INVOICE_ID);
        refund.setPaymentId(PAYMENT_ID);
        refund.setRefundId(REFUND_ID);

        assertTrue(ComparingReportsUtils.isEqualRefunds(statRefund, refund));

        refund.setRefundId(REFUND_ID + REFUND_ID);
        assertFalse(ComparingReportsUtils.isEqualRefunds(statRefund, refund));
    }

    @Test
    public void isEqualAdjustmentsTest() {
        StatAdjustment statAdjustment = new StatAdjustment();
        statAdjustment.setInvoiceId(INVOICE_ID);
        statAdjustment.setPaymentId(PAYMENT_ID);
        statAdjustment.setAdjustmentId(ADJUSTMENT_ID);

        Adjustment adjustment = new Adjustment();
        adjustment.setInvoiceId(INVOICE_ID);
        adjustment.setPaymentId(PAYMENT_ID);
        adjustment.setAdjustmentId(ADJUSTMENT_ID);

        assertTrue(ComparingReportsUtils.isEqualAdjustments(statAdjustment, adjustment));

        adjustment.setAdjustmentId(ADJUSTMENT_ID + ADJUSTMENT_ID);
        assertFalse(ComparingReportsUtils.isEqualAdjustments(statAdjustment, adjustment));
    }

}
