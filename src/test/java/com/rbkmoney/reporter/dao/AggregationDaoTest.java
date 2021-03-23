package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.domain.enums.*;
import com.rbkmoney.reporter.domain.tables.pojos.*;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PayoutAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundAggsByHourRecord;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.util.TestDataUtil.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

public class AggregationDaoTest extends AbstractDaoConfig {

    @Autowired
    private PayoutDao payoutDao;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @Test
    public void paymentsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int paymentsCount = 100;
        for (int i = 0; i < paymentsCount; i++) {
            paymentDao.savePayment(createTestPayment(partyId, shopId, LocalDateTime.now(), i));
        }
        paymentDao.aggregateForDate(LocalDateTime.now().minusHours(2L), LocalDateTime.now());
        List<PaymentAggsByHourRecord> paymentsAggregatesByHour =
                paymentDao.getPaymentsAggregatesByHour(LocalDateTime.now().minusHours(3L), LocalDateTime.now());
        assertEquals(2, paymentsAggregatesByHour.size());
        PaymentAggsByHourRecord paymentAggsByHourRecord = paymentsAggregatesByHour.get(0);
        assertEquals(Long.valueOf(50000L), paymentAggsByHourRecord.getAmount());
        assertEquals(Long.valueOf(25000L), paymentAggsByHourRecord.getFee());
    }

    @Test
    public void refundsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int refundsCount = 100;
        for (int i = 0; i < refundsCount; i++) {
            refundDao.saveRefund(createTestRefund(partyId, shopId, LocalDateTime.now(), i));
        }
        refundDao.aggregateForDate(LocalDateTime.now().minusHours(2L), LocalDateTime.now());
        List<RefundAggsByHourRecord> refundAggsByHour =
                refundDao.getRefundAggsByHour(LocalDateTime.now().minusHours(3L), LocalDateTime.now());
        assertEquals(2, refundAggsByHour.size());
        RefundAggsByHourRecord refundAggsByHourRecord = refundAggsByHour.get(0);
        assertEquals(Long.valueOf(50000L), refundAggsByHourRecord.getAmount());
        assertEquals(Long.valueOf(25000L), refundAggsByHourRecord.getFee());
    }

    @Test
    public void adjustmentsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int сount = 100;
        for (int i = 0; i < сount; i++) {
            adjustmentDao.saveAdjustment(createTestAdjustment(partyId, shopId, LocalDateTime.now(), i));
        }
        adjustmentDao.aggregateForDate(LocalDateTime.now().minusHours(2L), LocalDateTime.now());
        List<AdjustmentAggsByHourRecord> adjustmentsAggsByHour =
                adjustmentDao.getAdjustmentsAggsByHour(LocalDateTime.now().minusHours(3L), LocalDateTime.now());
        assertEquals(2, adjustmentsAggsByHour.size());
        AdjustmentAggsByHourRecord adjustmentAggsByHourRecord = adjustmentsAggsByHour.get(0);
        assertEquals(Long.valueOf(50000L), adjustmentAggsByHourRecord.getAmount());
    }

    @Test
    public void payoutsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int сount = 100;
        for (int i = 0; i < сount; i++) {
            Long extPayoutId = payoutDao.savePayout(createTestPayout(partyId, shopId, LocalDateTime.now(), i));
            PayoutState payoutState = createTestPayoutState(extPayoutId, LocalDateTime.now(), PayoutStatus.unpaid);
            payoutDao.savePayoutState(payoutState);
            payoutState.setStatus(PayoutStatus.paid);
            payoutDao.savePayoutState(payoutState);
        }

        payoutDao.aggregateForDate(LocalDateTime.now().minusHours(2L), LocalDateTime.now());
        List<PayoutAggsByHourRecord> payoutsAggsByHour =
                payoutDao.getPayoutsAggsByHour(LocalDateTime.now().minusHours(3L), LocalDateTime.now());
        assertEquals(2, payoutsAggsByHour.size());
        PayoutAggsByHourRecord payoutAggsByHourRecord = payoutsAggsByHour.get(0);
        assertEquals(Long.valueOf(50000L), payoutAggsByHourRecord.getAmount());
    }

}
