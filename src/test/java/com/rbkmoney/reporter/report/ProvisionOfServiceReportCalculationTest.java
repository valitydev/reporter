package com.rbkmoney.reporter.report;

import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.reporter.config.AbstractLocalTemplateConfig;
import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.data.ReportsTestData;
import com.rbkmoney.reporter.domain.enums.AggregationType;
import com.rbkmoney.reporter.domain.tables.pojos.*;
import com.rbkmoney.reporter.domain.tables.records.*;
import com.rbkmoney.reporter.model.PaymentFundsAmount;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.LocalStatisticService;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.StatisticService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProvisionOfServiceReportCalculationTest extends AbstractLocalTemplateConfig {

    @Autowired
    private LocalStatisticService localStatisticService;

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

    @Autowired
    private AggregatesDao aggregatesDao;

    @MockBean
    private InvoiceDao invoiceDao;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private ContractMetaDao contractMetaDao;

    @MockBean
    private PartyService partyService;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    private static final String TEST_PARTY_ID = "TestParty";
    private static final String TEST_SHOP_ID = "TestShop";

    @Test
    public void shopAccountingModelCreationTest() {
        int defaultCountOfOperations = 20;
        long paymentCreatedAtDelta = 14L;

        LocalDateTime createFrom = LocalDateTime.now();
        LocalDateTime firstPaymentOperationDatetime =
                createFrom.minusMinutes(paymentCreatedAtDelta * defaultCountOfOperations);

        List<PaymentRecord> paymentRecordList =
                createPaymentRecord(defaultCountOfOperations, 1000, 50, createFrom, paymentCreatedAtDelta);
        List<RefundRecord> refundRecordList =
                createRefundRecord(defaultCountOfOperations % 4, 700, createFrom, 540L);
        List<AdjustmentRecord> adjustmentRecordList =
                createAdjustmentRecord(defaultCountOfOperations % 5, 300, createFrom, 53L);
        List<PayoutRecord> payoutRecordList =
                createPayoutRecord(defaultCountOfOperations % 3, 250, createFrom, 23L);
        List<ChargebackRecord> chargebackRecordList =
                createChargebackData(defaultCountOfOperations % 10, 1000, createFrom, 27L);

        LocalDateTime dateFrom = firstPaymentOperationDatetime.plusMinutes(5L);
        LocalDateTime dateTo = createFrom.minusMinutes(5L);
        ShopAccountingModel expectedModel =
                new ShopAccountingModel(TEST_PARTY_ID, TEST_SHOP_ID, ReportsTestData.DEFAULT_CURRENCY);
        expectedModel.setFundsAcquired(getPaymentAmountForPeriod(paymentRecordList, dateFrom, dateTo));
        expectedModel.setFeeCharged(getPaymentFeeAmountForPeriod(paymentRecordList, dateFrom, dateTo));
        expectedModel.setFundsRefunded(getRefundAmountForPeriod(refundRecordList, dateFrom, dateTo));
        expectedModel.setFundsAdjusted(getAdjustmentAmountForPeriod(adjustmentRecordList, dateFrom, dateTo));
        expectedModel.setFundsPaidOut(getPayoutAmountForPeriod(payoutRecordList, dateFrom, dateTo));
        expectedModel.setFundsReturned(getChargebackAmountForPeriod(chargebackRecordList, dateFrom, dateTo));

        LocalDateTime aggDateFrom = dateFrom.minusHours(1L);
        LocalDateTime aggDateTo = dateTo.plusHours(1L);
        aggregatesDao.aggregateByHour(AggregationType.PAYMENT, aggDateFrom, aggDateTo);
        aggregatesDao.aggregateByHour(AggregationType.REFUND, aggDateFrom, aggDateTo);
        aggregatesDao.aggregateByHour(AggregationType.ADJUSTMENT, aggDateFrom, aggDateTo);
        aggregatesDao.aggregateByHour(AggregationType.PAYOUT, aggDateFrom, aggDateTo);

        ShopAccountingModel resultShopAccountingModel = localStatisticService.getShopAccounting(
                TEST_PARTY_ID,
                TEST_SHOP_ID,
                ReportsTestData.DEFAULT_CURRENCY,
                dateFrom,
                dateTo
        );
        assertEquals(expectedModel, resultShopAccountingModel);
    }

    @Test
    public void paymentStatisticsTest() {
        int paymentsCount = 10;
        long paymentsAmount = 250L;
        long paymentsFeeAmount = 200L;
        long paymentsCreationDelta = 17L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:29:03.786964");

        List<PaymentRecord> paymentRecordList =
                createPaymentRecord(paymentsCount, paymentsAmount, paymentsFeeAmount, createTo, paymentsCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(paymentsCreationDelta * paymentsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(paymentsCreationDelta * 2 + 10);

        long paymentAmountForPeriod = getPaymentAmountForPeriod(paymentRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.PAYMENT, firstPayoutDatetime, createTo);
        PaymentFundsAmount paymentFundsAmount = paymentDao.getPaymentFundsAmount(
                TEST_PARTY_ID, TEST_SHOP_ID, ReportsTestData.DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo
        );
        assertEquals(Long.valueOf(paymentAmountForPeriod), paymentFundsAmount.getFundsAcquiredAmount());
    }

    @Test
    public void refundStatisticsTest() {
        int refundsCount = 10;
        long refundsAmount = 250L;
        long refundsCreationDelta = 17L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:28:03.786964");
        List<RefundRecord> refundRecordList =
                createRefundRecord(refundsCount, refundsAmount, createTo, refundsCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(refundsCreationDelta * refundsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(refundsCreationDelta * 2 + 10);

        long refundAmountForPeriod = getRefundAmountForPeriod(refundRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.REFUND, firstPayoutDatetime, createTo);
        Long fundsRefundedAmount = refundDao.getFundsRefundedAmount(
                TEST_PARTY_ID, TEST_SHOP_ID, ReportsTestData.DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo
        );
        assertEquals(Long.valueOf(refundAmountForPeriod), fundsRefundedAmount);
    }

    @Test
    public void adjustmentStatisticsTest() {
        int adjustmentsCount = 19;
        long adjustmentsAmount = 250L;
        long adjustmentsCreationDelta = 21L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:28:03.786964");
        List<AdjustmentRecord> adjustmentRecordList =
                createAdjustmentRecord(adjustmentsCount, adjustmentsAmount, createTo, adjustmentsCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(adjustmentsCreationDelta * adjustmentsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(adjustmentsCreationDelta * 2 + 10);

        long adjustmentAmountForPeriod = getAdjustmentAmountForPeriod(adjustmentRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.ADJUSTMENT, firstPayoutDatetime, createTo);
        Long fundsAdjustedAmount = adjustmentDao.getFundsAdjustedAmount(
                TEST_PARTY_ID, TEST_SHOP_ID, ReportsTestData.DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo
        );
        assertEquals(Long.valueOf(adjustmentAmountForPeriod), fundsAdjustedAmount);
    }

    @Test
    public void payoutStatisticsTest() {
        int payoutsCount = 50;
        long payuotsAmount = 250L;
        long payoutsCreationDelta = 23L;
        LocalDateTime createTo = LocalDateTime.now();//.parse("2021-04-02T16:18:03.786964");
        List<PayoutRecord> payoutRecordList =
                createPayoutRecord(payoutsCount, payuotsAmount, createTo, payoutsCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(payoutsCreationDelta * payoutsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(payoutsCreationDelta * 2 + 10);

        long payoutAmountForPeriod = getPayoutAmountForPeriod(payoutRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.PAYOUT, firstPayoutDatetime, createTo);
        Long fundsPayOutAmount = payoutDao.getFundsPayOutAmount(
                TEST_PARTY_ID, TEST_SHOP_ID, ReportsTestData.DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo);
        assertEquals(Long.valueOf(payoutAmountForPeriod), fundsPayOutAmount);
    }

    @Test
    public void chargebackStatisticsTest() {
        int chargebacksCount = 10;
        long chargebacksAmount = 250L;
        long chargebacksCreationDelta = 25L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:58:03.786964");
        List<ChargebackRecord> chargebackRecordList =
                createChargebackData(chargebacksCount, chargebacksAmount, createTo, chargebacksCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(chargebacksCreationDelta * chargebacksCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(chargebacksCreationDelta * 2 + 10);

        long chargebackAmountForPeriod = getChargebackAmountForPeriod(chargebackRecordList, dateFrom, dateTo);
        Long fundsReturnedAmount = chargebackDao.getFundsReturnedAmount(
                TEST_PARTY_ID, TEST_SHOP_ID, ReportsTestData.DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo);
        assertEquals(Long.valueOf(chargebackAmountForPeriod), fundsReturnedAmount);
    }

    private long getPaymentFeeAmountForPeriod(List<PaymentRecord> paymentRecordList,
                                              LocalDateTime reportFrom,
                                              LocalDateTime reportTo) {
        return paymentRecordList.stream()
                .filter(payment -> payment.getStatusCreatedAt().isAfter(reportFrom)
                        || payment.getStatusCreatedAt().isEqual(reportFrom))
                .filter(payment -> payment.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(PaymentRecord::getFee)
                .sum();
    }

    private long getPaymentAmountForPeriod(List<PaymentRecord> paymentRecordList,
                                           LocalDateTime reportFrom,
                                           LocalDateTime reportTo) {
        return paymentRecordList.stream()
                .filter(payment -> payment.getStatusCreatedAt().isAfter(reportFrom)
                        || payment.getStatusCreatedAt().isEqual(reportFrom))
                .filter(payment -> payment.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(PaymentRecord::getAmount)
                .sum();
    }

    private long getRefundAmountForPeriod(List<RefundRecord> refundRecordList,
                                          LocalDateTime reportFrom,
                                          LocalDateTime reportTo) {
        return refundRecordList.stream()
                .filter(refund -> refund.getStatusCreatedAt().isAfter(reportFrom)
                        || refund.getStatusCreatedAt().isEqual(reportFrom))
                .filter(refund -> refund.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(RefundRecord::getAmount)
                .sum();
    }

    private long getAdjustmentAmountForPeriod(List<AdjustmentRecord> adjustmentRecordList,
                                              LocalDateTime reportFrom,
                                              LocalDateTime reportTo) {
        return adjustmentRecordList.stream()
                .filter(record -> record.getStatusCreatedAt().isAfter(reportFrom)
                        || record.getStatusCreatedAt().isEqual(reportFrom))
                .filter(record -> record.getStatusCreatedAt().isBefore(reportTo))
                .mapToLong(AdjustmentRecord::getAmount)
                .sum();
    }

    private long getPayoutAmountForPeriod(List<PayoutRecord> payoutRecordList,
                                          LocalDateTime reportFrom,
                                          LocalDateTime reportTo) {
        return payoutRecordList.stream()
                .filter(record -> record.getCreatedAt().isAfter(reportFrom)
                        || record.getCreatedAt().isEqual(reportFrom))
                .filter(record -> record.getCreatedAt().isBefore(reportTo))
                .mapToLong(PayoutRecord::getAmount)
                .sum();
    }

    private long getChargebackAmountForPeriod(List<ChargebackRecord> chargebackRecordList,
                                              LocalDateTime reportFrom,
                                              LocalDateTime reportTo) {
        return chargebackRecordList.stream()
                .filter(record -> record.getCreatedAt().isAfter(reportFrom)
                        || record.getCreatedAt().isEqual(reportFrom))
                .filter(record -> record.getCreatedAt().isBefore(reportTo))
                .mapToLong(ChargebackRecord::getAmount)
                .sum();
    }

    private List<ChargebackRecord> createChargebackData(int count,
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

    private List<PayoutRecord> createPayoutRecord(int count,
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

    private List<AdjustmentRecord> createAdjustmentRecord(int count,
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

    private List<RefundRecord> createRefundRecord(int count,
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

    private List<PaymentRecord> createPaymentRecord(int count,
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

}
