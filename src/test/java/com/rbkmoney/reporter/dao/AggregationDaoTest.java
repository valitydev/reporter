package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.domain.enums.AggregationType;
import com.rbkmoney.reporter.domain.enums.PayoutStatus;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PayoutAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundAggsByHourRecord;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.data.CommonTestData.*;
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

    @Autowired
    private AggregatesDao aggregatesDao;

    @Test
    public void paymentsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime dateTo = LocalDateTime.now();

        int paymentsCount = 100;
        for (int i = 0; i < paymentsCount; i++) {
            paymentDao.savePayment(createTestPayment(partyId, shopId, dateTo.minusHours(1L), i));
        }
        LocalDateTime dateFrom = dateTo.minusHours(3L);
        aggregatesDao.aggregateByHour(AggregationType.PAYMENT, dateFrom, dateTo);
        List<PaymentAggsByHourRecord> paymentsAggregatesByHour =
                aggregatesDao.getPaymentsAggregatesByHour(dateFrom, dateTo);
        assertEquals(2, paymentsAggregatesByHour.size());

        PaymentAggsByHourRecord paymentAggsByHourRecord = paymentsAggregatesByHour.get(0);
        assertEquals(Long.valueOf(50000L), paymentAggsByHourRecord.getAmount());
        assertEquals(Long.valueOf(25000L), paymentAggsByHourRecord.getFee());
    }

    @Test
    public void refundsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime dateTo = LocalDateTime.now();

        int refundsCount = 100;
        for (int i = 0; i < refundsCount; i++) {
            refundDao.saveRefund(createTestRefund(partyId, shopId, dateTo.minusHours(1L), i));
        }
        LocalDateTime dateFrom = dateTo.minusHours(3L);
        aggregatesDao.aggregateByHour(AggregationType.REFUND, dateFrom, dateTo);
        List<RefundAggsByHourRecord> refundAggsByHour =
                aggregatesDao.getRefundsAggregatesByHour(dateFrom, dateTo);
        assertEquals(2, refundAggsByHour.size());

        RefundAggsByHourRecord refundAggsByHourRecord = refundAggsByHour.get(0);
        assertEquals(Long.valueOf(50000L), refundAggsByHourRecord.getAmount());
        assertEquals(Long.valueOf(25000L), refundAggsByHourRecord.getFee());
    }

    @Test
    public void adjustmentsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime dateTo = LocalDateTime.now();

        int count = 100;
        for (int i = 0; i < count; i++) {
            adjustmentDao.saveAdjustment(createTestAdjustment(partyId, shopId, dateTo.minusHours(1L), i));
        }
        LocalDateTime dateFrom = dateTo.minusHours(3L);
        aggregatesDao.aggregateByHour(AggregationType.ADJUSTMENT, dateFrom, dateTo);
        List<AdjustmentAggsByHourRecord> adjustmentsAggsByHour =
                aggregatesDao.getAdjustmentsAggregatesByHour(dateFrom, dateTo);
        assertEquals(2, adjustmentsAggsByHour.size());

        AdjustmentAggsByHourRecord adjustmentAggsByHourRecord = adjustmentsAggsByHour.get(0);
        assertEquals(Long.valueOf(50000L), adjustmentAggsByHourRecord.getAmount());
    }

    @Test
    public void payoutsAggragateDaoTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime dateTo = LocalDateTime.now();

        int count = 10;
        for (int i = 0; i < count; i++) {
            LocalDateTime payoutDateTo = dateTo.minusHours(1L);
            Long extPayoutId = payoutDao.savePayout(createTestPayout(partyId, shopId, payoutDateTo, i));
            PayoutState payoutState = createTestPayoutState(extPayoutId, payoutDateTo, PayoutStatus.unpaid, i);
            payoutDao.savePayoutState(payoutState);
            payoutState.setStatus(PayoutStatus.paid);
            payoutDao.savePayoutState(payoutState);
        }

        LocalDateTime dateFrom = dateTo.minusHours(3L);
        aggregatesDao.aggregateByHour(AggregationType.PAYOUT, dateFrom, dateTo);
        List<PayoutAggsByHourRecord> payoutsAggsByHour =
                aggregatesDao.getPayoutsAggregatesByHour(dateFrom, dateTo);
        assertEquals(2, payoutsAggsByHour.size());

        PayoutAggsByHourRecord payoutAggsByHourRecord = payoutsAggsByHour.get(0);
        assertEquals(Long.valueOf(5000L), payoutAggsByHourRecord.getAmount());
    }

}
