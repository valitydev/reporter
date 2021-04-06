package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.domain.enums.PayoutStatus;
import com.rbkmoney.reporter.domain.tables.pojos.*;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PayoutAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundAggsByHourRecord;
import com.rbkmoney.reporter.service.impl.AggregationServiceImpl;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.AdjustmentAggsByHour.ADJUSTMENT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.PaymentAggsByHour.PAYMENT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.PayoutAggsByHour.PAYOUT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.RefundAggsByHour.REFUND_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.data.CommonTestData.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

public class AggregationServiceTest extends AbstractDaoConfig {

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

    @Autowired
    private HikariDataSource dataSource;

    private AggregationService aggregationService;

    private InvoicingTestDao invoicingTestDao;

    @Before
    public void setup() {
        aggregationService = new AggregationServiceImpl(aggregatesDao);
        invoicingTestDao = new InvoicingTestDao(dataSource);
    }

    @Test
    public void aggregatePaymentsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int count = 100;
        LocalDateTime dateTo = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            Payment testPayment = createTestPayment(partyId, shopId, dateTo.minusHours(1L), i);
            paymentDao.savePayment(testPayment);
        }

        LocalDateTime prevAggregateTime = dateTo.truncatedTo(ChronoUnit.HOURS).minusHours(2L);
        PaymentAggsByHour testPaymentAggsByHour = createTestPaymentAggsByHour(
                prevAggregateTime,
                partyId + 0,
                shopId + 0
        );
        invoicingTestDao.savePaymentsAggregate(testPaymentAggsByHour);
        PaymentAggsByHour testPaymentAggsByHourTwo = createTestPaymentAggsByHour(
                prevAggregateTime,
                partyId + 1,
                shopId + 1
        );
        invoicingTestDao.savePaymentsAggregate(testPaymentAggsByHourTwo);

        LocalDateTime dateFrom = dateTo.minusHours(3L);
        List<PaymentAggsByHourRecord> prevPaymentsAggregatesByHour =
                aggregatesDao.getPaymentsAggregatesByHour(dateFrom, dateTo);
        assertEquals(2, prevPaymentsAggregatesByHour.size());

        aggregationService.aggregatePayments();

        List<PaymentAggsByHourRecord> paymentsAggregatesByHour =
                aggregatesDao.getPaymentsAggregatesByHour(dateFrom, dateTo);
        assertEquals(4, paymentsAggregatesByHour.size());
    }

    @Test
    public void aggregateRefundsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int count = 100;
        for (int i = 0; i < count; i++) {
            refundDao.saveRefund(createTestRefund(partyId, shopId, LocalDateTime.now().minusHours(1L), i));
        }

        LocalDateTime prevAggregateTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(2L);
        invoicingTestDao.saveRefundsAggregate(
                createTestRefundAggsByHour(prevAggregateTime, partyId + 0, shopId + 0));
        invoicingTestDao.saveRefundsAggregate(
                createTestRefundAggsByHour(prevAggregateTime, partyId + 1, shopId + 1));

        List<RefundAggsByHourRecord> refundAggsByHour = aggregatesDao.getRefundsAggregatesByHour(
                LocalDateTime.now().minusHours(3L),
                LocalDateTime.now()
        );
        assertEquals(2, refundAggsByHour.size());

        aggregationService.aggregateRefunds();

        List<RefundAggsByHourRecord> refundAggsByHourLast = aggregatesDao.getRefundsAggregatesByHour(
                LocalDateTime.now().minusHours(3L),
                LocalDateTime.now()
        );
        assertEquals(4, refundAggsByHourLast.size());
    }

    @Test
    public void aggregateAdjustmentsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime dateTo = LocalDateTime.now();

        int count = 100;
        for (int i = 0; i < count; i++) {
            adjustmentDao.saveAdjustment(createTestAdjustment(partyId, shopId, dateTo.minusHours(1L), i));
        }

        LocalDateTime prevAggregateTime = dateTo.truncatedTo(ChronoUnit.HOURS).minusHours(2L);
        invoicingTestDao.saveAdjustmentsAggregate(
                createTestAdjAggsByHour(prevAggregateTime, partyId + 0, shopId + 0));
        invoicingTestDao.saveAdjustmentsAggregate(
                createTestAdjAggsByHour(prevAggregateTime, partyId + 1, shopId + 1));

        LocalDateTime dateFrom = dateTo.minusHours(3L);
        List<AdjustmentAggsByHourRecord> adjustmentsAggsByHour =
                aggregatesDao.getAdjustmentsAggregatesByHour(dateFrom, dateTo);
        assertEquals(2, adjustmentsAggsByHour.size());

        aggregationService.aggregateAdjustments();

        List<AdjustmentAggsByHourRecord> adjustmentsAggsByHourLast =
                aggregatesDao.getAdjustmentsAggregatesByHour(dateFrom, dateTo);
        assertEquals(4, adjustmentsAggsByHourLast.size());
    }

    @Test
    public void aggregatePayoutsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime dateTo = LocalDateTime.now();

        int count = 100;
        for (int i = 0; i < count; i++) {
            LocalDateTime createdAt = dateTo.minusHours(1L);
            Long extPayoutId = payoutDao.savePayout(createTestPayout(partyId, shopId, createdAt, i));
            payoutDao.savePayoutState(createTestPayoutState(extPayoutId, createdAt, PayoutStatus.unpaid, i));
            payoutDao.savePayoutState(createTestPayoutState(extPayoutId, createdAt, PayoutStatus.paid, i));
        }

        LocalDateTime prevAggregateTime = dateTo.truncatedTo(ChronoUnit.HOURS).minusHours(2L);
        invoicingTestDao.savePayoutsAggregate(
                createTestPayoutAggsByHour(prevAggregateTime, partyId + 0, shopId + 0));
        invoicingTestDao.savePayoutsAggregate(
                createTestPayoutAggsByHour(prevAggregateTime, partyId + 1, shopId + 1));

        LocalDateTime dateFrom = dateTo.minusHours(3L);
        List<PayoutAggsByHourRecord> payoutsAggsByHour =
                aggregatesDao.getPayoutsAggregatesByHour(dateFrom, dateTo);
        assertEquals(2, payoutsAggsByHour.size());

        aggregationService.aggregatePayouts();

        List<PayoutAggsByHourRecord> lastPayoutsAggsByHour =
                aggregatesDao.getPayoutsAggregatesByHour(dateFrom, dateTo);
        assertEquals(4, lastPayoutsAggsByHour.size());
    }

    private static class InvoicingTestDao {

        private final DSLContext dslContext;

        public InvoicingTestDao(DataSource dataSource) {
            Configuration configuration = new DefaultConfiguration();
            configuration.set(SQLDialect.POSTGRES);
            configuration.set(dataSource);
            this.dslContext = DSL.using(configuration);
        }

        public void savePaymentsAggregate(PaymentAggsByHour paymentAggsByHour) {
            dslContext.insertInto(PAYMENT_AGGS_BY_HOUR)
                    .set(dslContext.newRecord(PAYMENT_AGGS_BY_HOUR, paymentAggsByHour))
                    .execute();
        }

        public void saveRefundsAggregate(RefundAggsByHour refundAggsByHour) {
            dslContext.insertInto(REFUND_AGGS_BY_HOUR)
                    .set(dslContext.newRecord(REFUND_AGGS_BY_HOUR, refundAggsByHour))
                    .execute();
        }

        public void saveAdjustmentsAggregate(AdjustmentAggsByHour adjustmentAggsByHour) {
            dslContext.insertInto(ADJUSTMENT_AGGS_BY_HOUR)
                    .set(dslContext.newRecord(ADJUSTMENT_AGGS_BY_HOUR, adjustmentAggsByHour))
                    .execute();
        }

        public void savePayoutsAggregate(PayoutAggsByHour payoutAggsByHour) {
            dslContext.insertInto(PAYOUT_AGGS_BY_HOUR)
                    .set(dslContext.newRecord(PAYOUT_AGGS_BY_HOUR, payoutAggsByHour))
                    .execute();
        }

    }

}
