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
public class ProvisionOfServiceReportCalculationTest extends AbstractReportsCompatingTest {

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
                preparePaymentRecords(defaultCountOfOperations, 1000, 50, createFrom, paymentCreatedAtDelta);
        List<RefundRecord> refundRecordList =
                prepareRefundRecords(defaultCountOfOperations % 4, 700, createFrom, 540L);
        List<AdjustmentRecord> adjustmentRecordList =
                prepareAdjustmentRecords(defaultCountOfOperations % 5, 300, createFrom, 53L);
        List<PayoutRecord> payoutRecordList =
                preparePayoutRecords(defaultCountOfOperations % 3, 250, createFrom, 23L);
        List<ChargebackRecord> chargebackRecordList =
                prepareChargebackRecords(defaultCountOfOperations % 10, 1000, createFrom, 27L);

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

        List<PaymentRecord> paymentRecordList = preparePaymentRecords(
                paymentsCount, paymentsAmount, paymentsFeeAmount, createTo, paymentsCreationDelta
        );

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
                prepareRefundRecords(refundsCount, refundsAmount, createTo, refundsCreationDelta);

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
                prepareAdjustmentRecords(adjustmentsCount, adjustmentsAmount, createTo, adjustmentsCreationDelta);

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
        LocalDateTime createTo = LocalDateTime.now();
        List<PayoutRecord> payoutRecordList =
                preparePayoutRecords(payoutsCount, payuotsAmount, createTo, payoutsCreationDelta);

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
                prepareChargebackRecords(chargebacksCount, chargebacksAmount, createTo, chargebacksCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(chargebacksCreationDelta * chargebacksCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(chargebacksCreationDelta * 2 + 10);

        long chargebackAmountForPeriod = getChargebackAmountForPeriod(chargebackRecordList, dateFrom, dateTo);
        Long fundsReturnedAmount = chargebackDao.getFundsReturnedAmount(
                TEST_PARTY_ID, TEST_SHOP_ID, ReportsTestData.DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo);
        assertEquals(Long.valueOf(chargebackAmountForPeriod), fundsReturnedAmount);
    }

}
