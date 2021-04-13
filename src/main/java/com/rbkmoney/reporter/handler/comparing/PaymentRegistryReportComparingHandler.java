package com.rbkmoney.reporter.handler.comparing;

import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.dao.ReportComparingDataDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.rbkmoney.reporter.model.StatAdjustment;
import com.rbkmoney.reporter.service.LocalStatisticService;
import com.rbkmoney.reporter.service.StatisticService;
import org.jooq.Cursor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;

import static com.rbkmoney.reporter.util.ComparingReportsUtils.*;

@Component
public class PaymentRegistryReportComparingHandler extends ReportComparingAbstractHandler {

    private final StatisticService statisticService;
    private final LocalStatisticService localStatisticService;

    public PaymentRegistryReportComparingHandler(ReportComparingDataDao reportComparingDataDao,
                                                 StatisticService statisticService,
                                                 LocalStatisticService localStatisticService) {
        super(reportComparingDataDao);
        this.statisticService = statisticService;
        this.localStatisticService = localStatisticService;
    }

    @Override
    public void compareReport(Report report) {
        Long reportId = report.getId();
        String partyId = report.getPartyId();
        String shopId = report.getPartyShopId();
        LocalDateTime fromTime = report.getFromTime();
        LocalDateTime toTime = report.getToTime();

        try (
                Cursor<PaymentRecord> paymentsCursor =
                        localStatisticService.getPaymentsCursor(partyId, shopId, fromTime, toTime);
                Cursor<RefundRecord> refundsCursor =
                        localStatisticService.getRefundsCursor(partyId, shopId, fromTime, toTime);
                Cursor<AdjustmentRecord> adjustmentCursor =
                        localStatisticService.getAdjustmentCursor(partyId, shopId, fromTime, toTime)
        ) {
            Instant instantFromTime = fromTime.toInstant(ZoneOffset.UTC);
            Instant instantToTime = toTime.toInstant(ZoneOffset.UTC);

            Iterator<StatPayment> paymentsIterator =
                    statisticService.getCapturedPaymentsIterator(partyId, shopId, instantFromTime, instantToTime);
            Iterator<StatRefund> refundsIterator =
                    statisticService.getRefundsIterator(partyId, shopId, instantFromTime, instantToTime);
            Iterator<StatAdjustment> adjustmentsIterator =
                    statisticService.getAdjustmentsIterator(partyId, shopId, instantFromTime, instantToTime);

            if (isEqualPaymentsStreams(paymentsIterator, paymentsCursor, reportId)
                    && isEqualRefundsStreams(refundsIterator, refundsCursor, reportId)
                    && isEqualAdjustmentsStreams(adjustmentsIterator, adjustmentCursor, reportId)) {
                saveSuccessComparingInfo(reportId, ReportType.payment_registry);
            }
        }
    }

    private boolean isEqualPaymentsStreams(Iterator<StatPayment> paymentsIterator,
                                           Cursor<PaymentRecord> paymentsCursor,
                                           long reportId) {
        while (paymentsIterator.hasNext()) {
            StatPayment statPayment = paymentsIterator.next();

            if (paymentsCursor.hasNext()) {
                Payment payment = paymentsCursor.fetchNext().into(Payment.class);

                if (!isEqualPayments(statPayment, payment)) {
                    String failureReason = String.format("Payments don't equal (statPayment: %s, payment: %s)",
                            statPayment, payment);
                    saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
                    return false;
                }
            } else {
                String failureReason = String.format("Payment is empty, but StatPayment is not empty " +
                        "(StatPayment: %s)", statPayment);
                saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
                return false;
            }
        }
        if (paymentsCursor.hasNext()) {
            String failureReason = String.format("StatPayment is empty, but Payment is not empty " +
                    "(payment: %s)", paymentsCursor.fetchNext());
            saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
            return false;
        }
        return true;
    }

    private boolean isEqualRefundsStreams(Iterator<StatRefund> refundsIterator,
                                          Cursor<RefundRecord> refundsCursor,
                                          long reportId) {
        while (refundsIterator.hasNext()) {
            StatRefund statRefund = refundsIterator.next();

            if (refundsCursor.hasNext()) {
                Refund refund = refundsCursor.fetchNext().into(Refund.class);

                if (!isEqualRefunds(statRefund, refund)) {
                    String failureReason = String.format("Refunds don't equal (statRefund: %s, refund: %s)",
                            statRefund, refund);
                    saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
                    return false;
                }
            } else {
                String failureReason = String.format("Refund object is empty, but statRefund is not empty " +
                        "(statRefund: %s)", statRefund);
                saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
                return false;
            }
        }
        if (refundsCursor.hasNext()) {
            String failureReason = String.format("StatRefund is empty, but refund object is not empty " +
                    "(refund: %s)", refundsCursor.fetchNext());
            saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
            return false;
        }
        return true;
    }

    private boolean isEqualAdjustmentsStreams(Iterator<StatAdjustment> adjustmentIterator,
                                              Cursor<AdjustmentRecord> adjustmentsCursor,
                                              long reportId) {
        while (adjustmentIterator.hasNext()) {
            StatAdjustment statAdjustment = adjustmentIterator.next();

            if (adjustmentsCursor.hasNext()) {
                Adjustment adjustment = adjustmentsCursor.fetchNext().into(Adjustment.class);

                if (!isEqualAdjustments(statAdjustment, adjustment)) {
                    String failureReason = String.format("Adjustments don't equal (statAdjustment: %s, " +
                            "adjustment: %s)", statAdjustment, adjustment);
                    saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
                    return false;
                }
            } else {
                String failureReason = String.format("Adjustment object is empty, but statAdjustment is not empty " +
                        "(statAdjustment: %s)", statAdjustment);
                saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
                return false;
            }
        }
        if (adjustmentsCursor.hasNext()) {
            String failureReason = String.format("StatAdjustment is empty, but adjustment object is not empty " +
                    "(adjustment: %s)", adjustmentsCursor.fetchNext());
            saveErrorComparingInfo(failureReason, reportId, ReportType.payment_registry);
            return false;
        }
        return true;
    }
}
