package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.domain.enums.*;
import com.rbkmoney.reporter.domain.tables.pojos.*;
import com.rbkmoney.reporter.domain.tables.records.InvoiceRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.rbkmoney.reporter.exception.DaoException;
import org.awaitility.Awaitility;
import org.jooq.Cursor;
import org.jooq.Result;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DaoTest extends AbstractDaoConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private PayoutDao payoutDao;

    @Autowired
    private InvoiceDao invoiceDao;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private ContractMetaDao contractMetaDao;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute("truncate rpt.report cascade");
        jdbcTemplate.execute("truncate rpt.file_meta cascade");
        jdbcTemplate.execute("truncate rpt.contract_meta cascade");
    }

    @Test
    public void testSaveAndGet() throws DaoException {
        String partyId = "test";
        String contractId = "test";

        ContractMeta contractMeta = random(ContractMeta.class, "partyId", "contractId", "reportType");
        contractMeta.setPartyId(partyId);
        contractMeta.setContractId(contractId);

        contractMetaDao.save(contractMeta);
        ContractMeta contractMeta2 = contractMetaDao.get(contractMeta.getPartyId(), contractMeta.getContractId());
        assertEquals(contractMeta.getPartyId(), contractMeta2.getPartyId());
        assertEquals(contractMeta.getContractId(), contractMeta2.getContractId());
        assertEquals(contractMeta.getScheduleId(), contractMeta2.getScheduleId());
        assertEquals(contractMeta.getLastEventId(), contractMeta2.getLastEventId());
        assertEquals(contractMeta.getCalendarId(), contractMeta2.getCalendarId());

        assertEquals(contractMeta.getLastEventId(), contractMetaDao.getLastEventId());

        assertEquals(contractMeta2, contractMetaDao.getAllActiveContracts().get(0));

        contractMeta = random(ContractMeta.class, "partyId", "contractId", "reportType");
        contractMeta.setPartyId(partyId);
        contractMeta.setContractId(contractId);

        contractMetaDao.save(contractMeta);
        contractMeta2 = contractMetaDao.get(contractMeta.getPartyId(), contractMeta.getContractId());
        assertEquals(contractMeta.getPartyId(), contractMeta2.getPartyId());
        assertEquals(contractMeta.getContractId(), contractMeta2.getContractId());
        assertEquals(contractMeta.getScheduleId(), contractMeta2.getScheduleId());
        assertEquals(contractMeta.getLastEventId(), contractMeta2.getLastEventId());
        assertEquals(contractMeta.getCalendarId(), contractMeta2.getCalendarId());
    }

    @Test
    public void insertAndGetReportTest() throws DaoException {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime fromTime = LocalDateTime.now().minusDays(1);
        LocalDateTime toTime = LocalDateTime.now().plusDays(1);
        ReportType reportType = random(ReportType.class);
        String timezone = random(TimeZone.class).getID();
        LocalDateTime createdAt = LocalDateTime.now();

        long reportId = reportDao.createReport(partyId, shopId, fromTime, toTime, reportType, timezone, createdAt);

        Report report = reportDao.getReport(reportId);
        assertEquals(reportId, report.getId().longValue());
        assertEquals(partyId, report.getPartyId());
        assertEquals(shopId, report.getPartyShopId());
        assertEquals(fromTime, report.getFromTime());
        assertEquals(toTime, report.getToTime());
        assertEquals(reportType, report.getType());
        assertEquals(timezone, report.getTimezone());
        assertEquals(createdAt, report.getCreatedAt());

        assertEquals(1, reportDao.getReportsByRange(partyId, shopId, new ArrayList<>(), fromTime, toTime).size());

        assertEquals(1,
                reportDao.getReportsByRange(partyId, shopId, Arrays.asList(reportType), fromTime, toTime).size());

        assertEquals(1, reportDao.getReportsByRange(partyId, null, Arrays.asList(reportType), fromTime, toTime).size());

        reportDao.createReport(partyId, null, fromTime, toTime, reportType, timezone, createdAt);

        assertEquals(2, reportDao.getReportsByRange(partyId, null, Arrays.asList(reportType), fromTime, toTime).size());
    }

    @Test
    public void testGetReportsWithToken() throws DaoException {
        int count = 15;
        LocalDateTime currMoment = LocalDateTime.now();
        createReports(count, currMoment);

        List<Report> reports =
                reportDao.getReportsWithToken("partyId", Collections.singletonList("shopId"), Collections.emptyList(),
                        currMoment.minusMinutes(1), currMoment.plusMinutes(1), null, 10);
        assertEquals(10, reports.size());
        List<Report> reportsWithTime =
                reportDao.getReportsWithToken("partyId", Collections.singletonList("shopId"), Collections.emptyList(),
                        currMoment.minusMinutes(1), currMoment.plusMinutes(1),
                        currMoment.plusSeconds(count + 1).minusSeconds(10), 10);
        assertEquals(5, reportsWithTime.size());
    }

    @Test
    public void testGetReportsWithTokenByShopIds() throws DaoException {
        int count = 15;
        LocalDateTime currMoment = LocalDateTime.now();
        createReports(count, currMoment);

        List<Report> reports = reportDao.getReportsWithToken("partyId", List.of("shopId"), Collections.emptyList(),
                currMoment.minusMinutes(1), currMoment.plusMinutes(1), null, 10);
        assertEquals(10, reports.size());
        List<Report> reportsWithTime =
                reportDao.getReportsWithToken("partyId", List.of("shopId"), Collections.emptyList(),
                        currMoment.minusMinutes(1), currMoment.plusMinutes(1),
                        currMoment.plusSeconds(count + 1).minusSeconds(10), 10);
        assertEquals(5, reportsWithTime.size());
    }

    @Test
    public void checkCreatedStatus() throws DaoException {
        String partyId = random(String.class);
        String shopId = random(String.class);
        LocalDateTime fromTime = random(LocalDateTime.class);
        LocalDateTime toTime = random(LocalDateTime.class);
        ReportType reportType = random(ReportType.class);
        String timezone = random(TimeZone.class).getID();
        LocalDateTime createdAt = random(LocalDateTime.class);

        long reportId = reportDao.createReport(partyId, shopId, fromTime, toTime, reportType, timezone, createdAt);
        reportDao.changeReportStatus(reportId, ReportStatus.created);

        reportDao.getReport(reportId);
    }

    @Test
    public void attachFileTest() throws DaoException {
        FileMeta file = random(FileMeta.class);
        Long reportId = random(Long.class);

        String fileId = reportDao.attachFile(reportId, file);
        FileMeta currentFile = reportDao.getFile(fileId);

        assertEquals(file.getFileId(), currentFile.getFileId());
        assertEquals(reportId, currentFile.getReportId());
        assertEquals(file.getBucketId(), currentFile.getBucketId());
        assertEquals(file.getFilename(), currentFile.getFilename());
        assertEquals(file.getMd5(), currentFile.getMd5());
        assertEquals(file.getSha256(), currentFile.getSha256());

        List<FileMeta> files = reportDao.getReportFiles(reportId);
        assertEquals(1, files.size());

        currentFile = files.get(0);

        assertEquals(file.getFileId(), currentFile.getFileId());
        assertEquals(reportId, currentFile.getReportId());
        assertEquals(file.getBucketId(), currentFile.getBucketId());
        assertEquals(file.getFilename(), currentFile.getFilename());
        assertEquals(file.getMd5(), currentFile.getMd5());
        assertEquals(file.getSha256(), currentFile.getSha256());
    }

    @Test
    public void payoutGetTest() {
        Payout payout = random(Payout.class);
        payoutDao.savePayout(payout);
        Payout resultPayout = payoutDao.getPayout(payout.getPayoutId());
        assertEquals(payout, resultPayout);

        Long extPayoutId = resultPayout.getId();
        PayoutAccount payoutAccount = random(PayoutAccount.class);
        payoutAccount.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutAccountInfo(payoutAccount);
        PayoutAccount resultPayoutAccount = payoutDao.getPayoutAccount(extPayoutId);
        assertEquals(payoutAccount, resultPayoutAccount);

        PayoutInternationalAccount payoutInternationalAccount = random(PayoutInternationalAccount.class);
        payoutInternationalAccount.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutInternationalAccountInfo(payoutInternationalAccount);
        PayoutInternationalAccount internationalAccount = payoutDao.getPayoutInternationalAccount(extPayoutId);
        assertEquals(payoutInternationalAccount, internationalAccount);

        PayoutState payoutState = random(PayoutState.class);
        payoutState.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutState(payoutState);
        PayoutState resultPayoutState = payoutDao.getPayoutState(extPayoutId);
        assertEquals(payoutState, resultPayoutState);
    }

    @Test
    public void saveAndGetInvoiceTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        Invoice invoice = random(Invoice.class);
        invoice.setPartyId(partyId);
        invoice.setShopId(shopId + "\u0000" + "x");
        invoice.setStatus(InvoiceStatus.paid);
        invoiceDao.saveInvoice(invoice);

        InvoiceRecord invoiceRecordOne = invoiceDao.getInvoice(invoice.getInvoiceId());
        assertEquals(invoice, invoiceRecordOne.into(Invoice.class));
    }

    @Test
    public void saveAndGetPaymentTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int paymentsCount = 100;
        List<Payment> sourcePayments = new ArrayList<>();
        for (int i = 0; i < paymentsCount; i++) {
            Payment payment = random(Payment.class);
            payment.setShopId(shopId);
            payment.setPartyId(partyId);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setStatusCreatedAt(LocalDateTime.now());
            payment.setStatus(InvoicePaymentStatus.captured);
            sourcePayments.add(payment);
            paymentDao.savePayment(payment);
        }
        Payment firstPayment = sourcePayments.get(0);
        PaymentRecord payment =
                paymentDao.getPayment(partyId, shopId, firstPayment.getInvoiceId(), firstPayment.getPaymentId());
        assertEquals(firstPayment, payment.into(Payment.class));

        Cursor<PaymentRecord> paymentsCursor =
                paymentDao.getPaymentsCursor(partyId, shopId, Optional.empty(), LocalDateTime.now());
        List<Payment> resultPayments = new ArrayList<>();
        while (paymentsCursor.hasNext()) {
            Result<PaymentRecord> paymentRecords = paymentsCursor.fetchNext(10);
            resultPayments.addAll(paymentRecords.into(Payment.class));
        }
        assertEquals(paymentsCount, resultPayments.size());
    }

    @Test
    public void saveAndGetRefundTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int refundsCount = 100;
        List<Refund> sourceRefunds = new ArrayList<>();
        for (int i = 0; i < refundsCount; i++) {
            Refund refund = random(Refund.class);
            refund.setShopId(shopId);
            refund.setPartyId(partyId);
            refund.setCreatedAt(LocalDateTime.now());
            refund.setStatusCreatedAt(LocalDateTime.now());
            refund.setStatus(RefundStatus.succeeded);
            sourceRefunds.add(refund);
            refundDao.saveRefund(refund);
        }

        Cursor<RefundRecord> refundsCursor = refundDao.getRefundsCursor(
                partyId,
                shopId,
                LocalDateTime.now().minus(10L, ChronoUnit.HOURS),
                LocalDateTime.now()
        );
        List<Refund> resultRefunds = new ArrayList<>();
        int iterationsCount = 0;
        while (refundsCursor.hasNext()) {
            Result<RefundRecord> refundRecords = refundsCursor.fetchNext(10);
            resultRefunds.addAll(refundRecords.into(Refund.class));
            iterationsCount++;
        }
        assertEquals(10, iterationsCount);
        assertEquals(refundsCount, resultRefunds.size());
    }

    @Test
    @Ignore("disable db lock test, the test lasts more than 2 minutes")
    public void severalInstancesReportServiceTest() throws ExecutionException, InterruptedException {
        Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(Duration.ofMinutes(1L));
        int count = 3000;
        createReports(count, LocalDateTime.now());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<List<Report>> firstFuture = executorService.submit(() -> reportDao.getPendingReports(count));
        Future<List<Report>> secondFuture = executorService.submit(() -> reportDao.getPendingReports(count));

        await().until(firstFuture::isDone);
        await().until(secondFuture::isDone);

        assertFalse(firstFuture.get().isEmpty());
        assertFalse(secondFuture.get().isEmpty());
    }

    private void createReports(int count, LocalDateTime currMoment) {
        IntStream.rangeClosed(1, count).forEach(i -> createReport(currMoment, i));
    }

    private void createReport(LocalDateTime currMoment, int i) {
        try {
            reportDao.createReport(
                    "partyId",
                    "shopId",
                    currMoment.minusSeconds(i),
                    currMoment.plusSeconds(i),
                    random(ReportType.class),
                    random(TimeZone.class).getID(),
                    currMoment.plusSeconds(i)
            );
        } catch (DaoException e) {
            throw new RuntimeException();
        }
    }
}
