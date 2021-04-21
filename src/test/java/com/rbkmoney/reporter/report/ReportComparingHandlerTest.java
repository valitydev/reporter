package com.rbkmoney.reporter.report;

import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.data.ReportsTestData;
import com.rbkmoney.reporter.domain.enums.AggregationType;
import com.rbkmoney.reporter.domain.enums.ComparingStatus;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.*;
import com.rbkmoney.reporter.domain.tables.records.*;
import com.rbkmoney.reporter.handler.comparing.PaymentRegistryReportComparingHandler;
import com.rbkmoney.reporter.handler.comparing.ProvisionOfServiceReportComparingHandler;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.model.StatAdjustment;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.StatisticService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class ReportComparingHandlerTest extends AbstractReportsCompatingTest {

    @Autowired
    private PaymentRegistryReportComparingHandler paymentRegistryReportComparingHandler;

    @Autowired
    private ProvisionOfServiceReportComparingHandler provisionOfServiceReportComparingHandler;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @Autowired
    private AggregatesDao aggregatesDao;

    @Autowired
    private ReportComparingDataDao reportComparingDataDao;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private InvoiceDao invoiceDao;

    @MockBean
    private ContractMetaDao contractMetaDao;

    @MockBean
    private PartyService partyService;

    @Test
    public void paymentRegistryReportComparingHandlerTest() throws InterruptedException {
        Thread.sleep(5000L);
        String partyId = random(String.class);
        String shopId = random(String.class);
        preparePaymentRegistryReportComparingHandlerTestData(partyId, shopId);
        long id = 3001L;
        paymentRegistryReportComparingHandler.compareReport(
                createTestReport(id, partyId, shopId, LocalDateTime.now().minusHours(5L), LocalDateTime.now())
        );
        ReportComparingData reportComparingData =
                reportComparingDataDao.getReportComparingDataByReportId(id);

        assertNotNull(reportComparingData);
        assertEquals(ComparingStatus.SUCCESS, reportComparingData.getStatus());
    }

    @Test
    public void provisionOfServiceReportComparingHandlerTest() throws InterruptedException {
        Thread.sleep(5000L);
        String partyId = random(String.class);
        String shopId = random(String.class);
        Report testReport = prepareProvisionOfServiceReportComparingHandlerTestData(partyId, shopId);
        long id = testReport.getId();
        provisionOfServiceReportComparingHandler.compareReport(testReport);
        ReportComparingData reportComparingData =
                reportComparingDataDao.getReportComparingDataByReportId(id);

        assertNotNull(reportComparingData);
        assertEquals(ComparingStatus.SUCCESS, reportComparingData.getStatus());
    }

    private void preparePaymentRegistryReportComparingHandlerTestData(String partyId, String shopId) {
        List<StatPayment> statPaymentList = new ArrayList<>();
        List<PaymentRecord> paymentRecordList = new ArrayList<>();
        int defaultOperationsCount = 3;
        for (int i = 0; i < defaultOperationsCount; ++i) {
            LocalDateTime createdAt = LocalDateTime.now().minusHours(defaultOperationsCount + 1 - i);
            statPaymentList.add(ReportsTestData.buildStatPayment(i, shopId, createdAt));

            PaymentRecord paymentRecord = ReportsTestData.buildPaymentRecord(i, partyId, shopId, createdAt);
            paymentRecordList.add(paymentRecord);

            paymentDao.savePayment(paymentRecord.into(Payment.class));
        }

        List<StatRefund> statRefundList = new ArrayList<>();
        List<RefundRecord> refundRecordList = new ArrayList<>();
        for (int i = 0; i < defaultOperationsCount; ++i) {
            LocalDateTime createdAt = LocalDateTime.now().minusHours(defaultOperationsCount + 1 - i);
            statRefundList.add(ReportsTestData.buildStatRefund(i, shopId, createdAt));

            RefundRecord refundRecord =
                    ReportsTestData.buildRefundRecord(i, partyId, shopId, 123L + i, createdAt);
            refundRecordList.add(refundRecord);

            refundDao.saveRefund(refundRecord.into(Refund.class));
        }

        List<StatAdjustment> adjustmentList = new ArrayList<>();
        List<AdjustmentRecord> adjustmentRecordList = new ArrayList<>();
        for (int i = 0; i < defaultOperationsCount; ++i) {
            LocalDateTime createdAt = LocalDateTime.now().minusHours(defaultOperationsCount + 1 - i);
            adjustmentList.add(ReportsTestData.buildStatAdjustment(i, shopId, createdAt));

            AdjustmentRecord adjustmentRecord =
                    ReportsTestData.buildStatAdjustmentRecord(i, partyId, shopId, 123L + i, createdAt);
            adjustmentRecordList.add(adjustmentRecord);

            adjustmentDao.saveAdjustment(adjustmentRecord.into(Adjustment.class));
        }

        given(statisticService.getCapturedPaymentsIterator(any(), any(), any(), any()))
                .willReturn(statPaymentList.iterator());
        given(statisticService.getRefundsIterator(any(), any(), any(), any()))
                .willReturn(statRefundList.iterator());
        given(statisticService.getAdjustmentsIterator(any(), any(), any(), any()))
                .willReturn(adjustmentList.iterator());
    }

    private Report prepareProvisionOfServiceReportComparingHandlerTestData(String partyId, String shopId) {
        int defaultCountOfOperations = 20;
        long paymentCreatedAtDelta = 14L;

        LocalDateTime createFrom = LocalDateTime.now();
        LocalDateTime firstPaymentOperationDatetime =
                createFrom.minusMinutes(paymentCreatedAtDelta * defaultCountOfOperations);

        List<PaymentRecord> paymentRecordList = preparePaymentRecords(
                partyId, shopId, defaultCountOfOperations, 1000, 50, createFrom, paymentCreatedAtDelta
        );
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
        LocalDateTime aggDateFrom = dateFrom.minusHours(1L);
        LocalDateTime aggDateTo = dateTo.plusHours(1L);
        aggregatesDao.aggregateByHour(AggregationType.PAYMENT, aggDateFrom, aggDateTo);
        aggregatesDao.aggregateByHour(AggregationType.REFUND, aggDateFrom, aggDateTo);
        aggregatesDao.aggregateByHour(AggregationType.ADJUSTMENT, aggDateFrom, aggDateTo);
        aggregatesDao.aggregateByHour(AggregationType.PAYOUT, aggDateFrom, aggDateTo);

        ShopAccountingModel expectedModel =
                new ShopAccountingModel(partyId, shopId, ReportsTestData.DEFAULT_CURRENCY);
        expectedModel.setFundsAcquired(getPaymentAmountForPeriod(paymentRecordList, dateFrom, dateTo));
        expectedModel.setFeeCharged(getPaymentFeeAmountForPeriod(paymentRecordList, dateFrom, dateTo));
        expectedModel.setFundsRefunded(getRefundAmountForPeriod(refundRecordList, dateFrom, dateTo));
        expectedModel.setFundsAdjusted(getAdjustmentAmountForPeriod(adjustmentRecordList, dateFrom, dateTo));
        expectedModel.setFundsPaidOut(getPayoutAmountForPeriod(payoutRecordList, dateFrom, dateTo));
        expectedModel.setFundsReturned(getChargebackAmountForPeriod(chargebackRecordList, dateFrom, dateTo));

        given(statisticService.getShopAccounting(any(), any(), any(), any(), any()))
                .willReturn(expectedModel);

        ShopAccountingModel balancesExpectedModel =
                new ShopAccountingModel(partyId, shopId, ReportsTestData.DEFAULT_CURRENCY);
        balancesExpectedModel.setFundsAcquired(1000L);
        balancesExpectedModel.setFeeCharged(50L);
        given(statisticService.getShopAccounting(any(), any(), any(), any()))
                .willReturn(balancesExpectedModel);

        return createTestReport(random(Long.class), partyId, shopId, dateFrom, dateTo);
    }

    private Report createTestReport(long id,
                                    String partyId,
                                    String shopId,
                                    LocalDateTime fromTime,
                                    LocalDateTime toTime) {
        Report report = new Report();
        report.setId(id);
        report.setPartyId(partyId);
        report.setPartyShopId(shopId);
        report.setFromTime(fromTime);
        report.setToTime(toTime);
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus(ReportStatus.created);
        report.setTimezone("UTC");
        report.setType(ReportType.provision_of_service);
        return report;
    }

}