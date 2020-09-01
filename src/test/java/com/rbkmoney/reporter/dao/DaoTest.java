package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
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

        assertEquals(1, reportDao.getReportsByRange(partyId, shopId, Arrays.asList(reportType), fromTime, toTime).size());

        assertEquals(1, reportDao.getReportsByRange(partyId, null, Arrays.asList(reportType), fromTime, toTime).size());

        reportDao.createReport(partyId, null, fromTime, toTime, reportType, timezone, createdAt);

        assertEquals(2, reportDao.getReportsByRange(partyId, null, Arrays.asList(reportType), fromTime, toTime).size());
    }

    @Test
    public void testGetReportsWithToken() throws DaoException {
        LocalDateTime currMoment = LocalDateTime.now();
        createReports(15, currMoment);

        List<Report> reports = reportDao.getReportsWithToken("partyId", Collections.singletonList("shopId"), Collections.emptyList(),
                currMoment.minusMinutes(1), currMoment.plusMinutes(1), null, 10);
        assertEquals(10, reports.size());
        List<Report> reportsWithTime = reportDao.getReportsWithToken("partyId", Collections.singletonList("shopId"), Collections.emptyList(),
                currMoment.minusMinutes(1), currMoment.plusMinutes(1), currMoment.plusSeconds(10), 10);
        assertEquals(5, reportsWithTime.size());
    }

    @Test
    public void testGetReportsWithTokenByShopIds() throws DaoException {
        LocalDateTime currMoment = LocalDateTime.now();
        createReports(15, currMoment);

        List<Report> reports = reportDao.getReportsWithToken("partyId", List.of("shopId"), Collections.emptyList(),
                currMoment.minusMinutes(1), currMoment.plusMinutes(1), null, 10);
        assertEquals(10, reports.size());
        List<Report> reportsWithTime = reportDao.getReportsWithToken("partyId", List.of("shopId"), Collections.emptyList(),
                currMoment.minusMinutes(1), currMoment.plusMinutes(1), currMoment.plusSeconds(10), 10);
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
    }

    @Test
    @Ignore("disable db lock test, the test lasts more than 2 minutes")
    public void severalInstancesReportServiceTest() throws ExecutionException, InterruptedException {
        Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(Duration.ONE_MINUTE);
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
