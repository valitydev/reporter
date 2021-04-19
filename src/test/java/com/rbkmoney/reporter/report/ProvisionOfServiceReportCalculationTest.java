package com.rbkmoney.reporter.report;

import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.domain.enums.AggregationType;
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
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.reporter.data.ReportsTestData.DEFAULT_CURRENCY;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
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

    @Test
    public void shopAccountingModelCreationTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        int defaultCountOfOperations = 20;
        long paymentCreatedAtDelta = 14L;

        LocalDateTime createFrom = LocalDateTime.now();
        LocalDateTime firstPaymentOperationDatetime =
                createFrom.minusMinutes(paymentCreatedAtDelta * defaultCountOfOperations);

        List<PaymentRecord> paymentRecordList = preparePaymentRecords(
                partyId, shopId, defaultCountOfOperations, 1000, 50, createFrom, paymentCreatedAtDelta);
        List<RefundRecord> refundRecordList = prepareRefundRecords(
                partyId, shopId, defaultCountOfOperations % 4, 700, createFrom, 540L);
        List<AdjustmentRecord> adjustmentRecordList = prepareAdjustmentRecords(
                partyId, shopId, defaultCountOfOperations % 5, 300, createFrom, 53L);
        List<PayoutRecord> payoutRecordList = preparePayoutRecords(
                partyId, shopId, defaultCountOfOperations % 3, 250, createFrom, 23L);
        List<ChargebackRecord> chargebackRecordList = prepareChargebackRecords(
                partyId, shopId, defaultCountOfOperations % 10, 1000, createFrom, 27L);

        LocalDateTime dateFrom = firstPaymentOperationDatetime.plusMinutes(5L);
        LocalDateTime dateTo = createFrom.minusMinutes(5L);
        ShopAccountingModel expectedModel =
                new ShopAccountingModel(partyId, shopId, DEFAULT_CURRENCY);
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

        ShopAccountingModel resultShopAccountingModel =
                localStatisticService.getShopAccounting(partyId, shopId, DEFAULT_CURRENCY, dateFrom, dateTo);
        assertEquals(expectedModel, resultShopAccountingModel);
    }

    @Test
    public void paymentStatisticsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        int paymentsCount = 10;
        long amount = 250L;
        long feeAmount = 200L;
        long creationDelta = 17L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:29:03.786964");

        List<PaymentRecord> paymentRecordList = preparePaymentRecords(
                partyId, shopId, paymentsCount, amount, feeAmount, createTo, creationDelta
        );

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(creationDelta * paymentsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(creationDelta * 2 + 10);

        long paymentAmountForPeriod = getPaymentAmountForPeriod(paymentRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.PAYMENT, firstPayoutDatetime, createTo);
        PaymentFundsAmount paymentFundsAmount = paymentDao.getPaymentFundsAmount(
                partyId, shopId, DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo
        );
        assertEquals(Long.valueOf(paymentAmountForPeriod), paymentFundsAmount.getFundsAcquiredAmount());
    }

    @Test
    public void refundStatisticsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        int refundsCount = 10;
        long refundsAmount = 250L;
        long refundsCreationDelta = 17L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:28:03.786964");
        List<RefundRecord> refundRecordList = prepareRefundRecords(
                partyId, shopId, refundsCount, refundsAmount, createTo, refundsCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(refundsCreationDelta * refundsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(refundsCreationDelta * 2 + 10);

        long refundAmountForPeriod = getRefundAmountForPeriod(refundRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.REFUND, firstPayoutDatetime, createTo);
        Long fundsRefundedAmount = refundDao.getFundsRefundedAmount(
                partyId, shopId, DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo
        );
        assertEquals(Long.valueOf(refundAmountForPeriod), fundsRefundedAmount);
    }

    @Test
    public void adjustmentStatisticsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        int adjustmentsCount = 19;
        long adjustmentsAmount = 250L;
        long adjustmentsCreationDelta = 21L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:28:03.786964");
        List<AdjustmentRecord> adjustmentRecordList = prepareAdjustmentRecords(
                partyId, shopId, adjustmentsCount, adjustmentsAmount, createTo, adjustmentsCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(adjustmentsCreationDelta * adjustmentsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(adjustmentsCreationDelta * 2 + 10);

        long adjustmentAmountForPeriod = getAdjustmentAmountForPeriod(adjustmentRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.ADJUSTMENT, firstPayoutDatetime, createTo);
        Long fundsAdjustedAmount = adjustmentDao.getFundsAdjustedAmount(
                partyId, shopId, DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo
        );
        assertEquals(Long.valueOf(adjustmentAmountForPeriod), fundsAdjustedAmount);
    }

    @Test
    public void payoutStatisticsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        int payoutsCount = 50;
        long payuotsAmount = 250L;
        long payoutsCreationDelta = 23L;
        LocalDateTime createTo = LocalDateTime.now();
        List<PayoutRecord> payoutRecordList = preparePayoutRecords(
                partyId, shopId, payoutsCount, payuotsAmount, createTo, payoutsCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(payoutsCreationDelta * payoutsCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(payoutsCreationDelta * 2 + 10);

        long payoutAmountForPeriod = getPayoutAmountForPeriod(payoutRecordList, dateFrom, dateTo);
        aggregatesDao.aggregateByHour(AggregationType.PAYOUT, firstPayoutDatetime, createTo);
        Long fundsPayOutAmount = payoutDao.getFundsPayOutAmount(
                partyId, shopId, DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo);
        assertEquals(Long.valueOf(payoutAmountForPeriod), fundsPayOutAmount);
    }

    @Test
    public void chargebackStatisticsTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        int chargebacksCount = 10;
        long chargebacksAmount = 250L;
        long chargebacksCreationDelta = 25L;
        LocalDateTime createTo = LocalDateTime.parse("2021-04-02T16:58:03.786964");
        List<ChargebackRecord> chargebackRecordList = prepareChargebackRecords(
                partyId, shopId,chargebacksCount, chargebacksAmount, createTo, chargebacksCreationDelta);

        LocalDateTime firstPayoutDatetime = createTo.minusMinutes(chargebacksCreationDelta * chargebacksCount);
        LocalDateTime dateFrom = firstPayoutDatetime.plusMinutes(10);
        LocalDateTime dateTo = createTo.minusMinutes(chargebacksCreationDelta * 2 + 10);

        long chargebackAmountForPeriod = getChargebackAmountForPeriod(chargebackRecordList, dateFrom, dateTo);
        Long fundsReturnedAmount = chargebackDao.getFundsReturnedAmount(
                partyId, shopId, DEFAULT_CURRENCY, Optional.of(dateFrom), dateTo);
        assertEquals(Long.valueOf(chargebackAmountForPeriod), fundsReturnedAmount);
    }

}
