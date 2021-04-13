package com.rbkmoney.reporter.report;

import com.rbkmoney.reporter.config.AbstractLocalTemplateConfig;
import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.data.ReportsTestData;
import com.rbkmoney.reporter.domain.tables.pojos.*;
import com.rbkmoney.reporter.domain.tables.records.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractReportsCompatingTest extends AbstractLocalTemplateConfig {

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @Autowired
    private PayoutDao payoutDao;

    @Autowired
    private ChargebackDao chargebackDao;

    static final String TEST_PARTY_ID = "TestParty";

    static final String TEST_SHOP_ID = "TestShop";

    List<PayoutRecord> preparePayoutRecords(int count,
                                            long amount,
                                            LocalDateTime startFrom,
                                            long createdAtDelta) {
        List<PayoutRecord> payoutRecordList = new ArrayList<>();
        LocalDateTime createdAt = startFrom.minusMinutes(createdAtDelta);
        for (int i = 0; i < count; ++i) {
            PayoutRecord payoutRecord =
                    ReportsTestData.buildPayoutRecord(i, TEST_PARTY_ID, TEST_SHOP_ID, amount, createdAt);
            payoutRecordList.add(payoutRecord);
            payoutDao.savePayout(payoutRecord.into(Payout.class));
            PayoutStateRecord payoutStateRecord = ReportsTestData.buildPayoutStateRecord(i, createdAt);
            payoutDao.savePayoutState(payoutStateRecord.into(PayoutState.class));
            createdAt = createdAt.minusMinutes(createdAtDelta);
        }
        return payoutRecordList;
    }

    List<AdjustmentRecord> prepareAdjustmentRecords(int count,
                                                    long amount,
                                                    LocalDateTime startFrom,
                                                    long createdAtDelta) {
        List<AdjustmentRecord> adjustmentRecordList = new ArrayList<>();
        LocalDateTime createdAt = startFrom.minusMinutes(createdAtDelta);
        for (int i = 0; i < count; ++i) {
            AdjustmentRecord adjustmentRecord =
                    ReportsTestData.buildStatAdjustmentRecord(i, TEST_PARTY_ID, TEST_SHOP_ID, amount, createdAt);
            adjustmentRecordList.add(adjustmentRecord);
            adjustmentDao.saveAdjustment(adjustmentRecord.into(Adjustment.class));
            createdAt = createdAt.minusMinutes(createdAtDelta);
        }
        return adjustmentRecordList;
    }

    List<RefundRecord> prepareRefundRecords(int count,
                                            long amount,
                                            LocalDateTime startFrom,
                                            long createdAtDelta) {
        List<RefundRecord> refundRecordList = new ArrayList<>();
        LocalDateTime createdAt = startFrom.minusMinutes(createdAtDelta);
        for (int i = 0; i < count; ++i) {
            RefundRecord refundRecord =
                    ReportsTestData.buildRefundRecord(i, TEST_PARTY_ID, TEST_SHOP_ID, amount, createdAt);
            refundRecordList.add(refundRecord);
            refundDao.saveRefund(refundRecord.into(Refund.class));
            createdAt = createdAt.minusMinutes(createdAtDelta);
        }
        return refundRecordList;
    }

    List<PaymentRecord> preparePaymentRecords(int count,
                                              long amount,
                                              long feeAmount,
                                              LocalDateTime startFrom,
                                              long createdAtDelta) {
        List<PaymentRecord> paymentRecordList = new ArrayList<>();
        LocalDateTime createdAt = startFrom.minusMinutes(createdAtDelta);
        for (int i = count; i > 0; i--) {
            PaymentRecord paymentRecord =
                    ReportsTestData.buildPaymentRecord(i, TEST_PARTY_ID, TEST_SHOP_ID, amount, feeAmount, createdAt);
            paymentRecordList.add(paymentRecord);
            paymentDao.savePayment(paymentRecord.into(Payment.class));
            createdAt = createdAt.minusMinutes(createdAtDelta);
        }
        return paymentRecordList;
    }

    List<ChargebackRecord> prepareChargebackRecords(int count,
                                                    long amount,
                                                    LocalDateTime startFrom,
                                                    long createdAtDelta) {
        List<ChargebackRecord> chargebackRecordList = new ArrayList<>();
        LocalDateTime createdAt = startFrom.minusMinutes(createdAtDelta);
        for (int i = 0; i < count; ++i) {
            ChargebackRecord chargebackRecord =
                    ReportsTestData.buildChargebackRecord(i, TEST_PARTY_ID, TEST_SHOP_ID, amount, createdAt);
            chargebackRecordList.add(chargebackRecord);
            chargebackDao.saveChargeback(chargebackRecord.into(Chargeback.class));
            createdAt = createdAt.minusMinutes(createdAtDelta);
        }
        return chargebackRecordList;
    }


    long getPaymentFeeAmountForPeriod(List<PaymentRecord> paymentRecordList,
                                      LocalDateTime reportFrom,
                                      LocalDateTime reportTo) {
        return paymentRecordList.stream()
                .filter(payment -> payment.getStatusCreatedAt().isAfter(reportFrom)
                        || payment.getStatusCreatedAt().isEqual(reportFrom))
                .filter(payment -> payment.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(PaymentRecord::getFee)
                .sum();
    }

    long getPaymentAmountForPeriod(List<PaymentRecord> paymentRecordList,
                                   LocalDateTime reportFrom,
                                   LocalDateTime reportTo) {
        return paymentRecordList.stream()
                .filter(payment -> payment.getStatusCreatedAt().isAfter(reportFrom)
                        || payment.getStatusCreatedAt().isEqual(reportFrom))
                .filter(payment -> payment.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(PaymentRecord::getAmount)
                .sum();
    }

    long getRefundAmountForPeriod(List<RefundRecord> refundRecordList,
                                  LocalDateTime reportFrom,
                                  LocalDateTime reportTo) {
        return refundRecordList.stream()
                .filter(refund -> refund.getStatusCreatedAt().isAfter(reportFrom)
                        || refund.getStatusCreatedAt().isEqual(reportFrom))
                .filter(refund -> refund.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(RefundRecord::getAmount)
                .sum();
    }

    long getAdjustmentAmountForPeriod(List<AdjustmentRecord> adjustmentRecordList,
                                      LocalDateTime reportFrom,
                                      LocalDateTime reportTo) {
        return adjustmentRecordList.stream()
                .filter(record -> record.getStatusCreatedAt().isAfter(reportFrom)
                        || record.getStatusCreatedAt().isEqual(reportFrom))
                .filter(record -> record.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(AdjustmentRecord::getAmount)
                .sum();
    }

    long getPayoutAmountForPeriod(List<PayoutRecord> payoutRecordList,
                                  LocalDateTime reportFrom,
                                  LocalDateTime reportTo) {
        return payoutRecordList.stream()
                .filter(record -> record.getCreatedAt().isAfter(reportFrom)
                        || record.getCreatedAt().isEqual(reportFrom))
                .filter(record -> record.getCreatedAt().isBefore(reportTo))
                .mapToLong(PayoutRecord::getAmount)
                .sum();
    }

    long getChargebackAmountForPeriod(List<ChargebackRecord> chargebackRecordList,
                                      LocalDateTime reportFrom,
                                      LocalDateTime reportTo) {
        return chargebackRecordList.stream()
                .filter(record -> record.getCreatedAt().isAfter(reportFrom)
                        || record.getCreatedAt().isEqual(reportFrom))
                .filter(record -> record.getCreatedAt().isBefore(reportTo))
                .mapToLong(ChargebackRecord::getAmount)
                .sum();
    }

}
