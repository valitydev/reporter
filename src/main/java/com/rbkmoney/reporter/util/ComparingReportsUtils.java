package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.model.StatAdjustment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ComparingReportsUtils {

    public static boolean isEqualPayments(StatPayment statPayment, Payment payment) {
        if (statPayment == null || payment == null) {
            return false;
        }

        return statPayment.getInvoiceId().equals(payment.getInvoiceId())
                && statPayment.getId().equals(payment.getPaymentId());
    }

    public static boolean isEqualRefunds(StatRefund statRefund, Refund refund) {
        if (statRefund == null || refund == null) {
            return false;
        }

        return statRefund.getInvoiceId().equals(refund.getInvoiceId())
                && statRefund.getPaymentId().equals(refund.getPaymentId())
                && statRefund.getId().equals(refund.getRefundId());
    }

    public static boolean isEqualAdjustments(StatAdjustment statAdjustment, Adjustment adjustment) {
        if (statAdjustment == null || adjustment == null) {
            return false;
        }

        return statAdjustment.getInvoiceId().equals(adjustment.getInvoiceId())
                && statAdjustment.getPaymentId().equals(adjustment.getPaymentId())
                && statAdjustment.getAdjustmentId().equals(adjustment.getAdjustmentId());
    }

}
