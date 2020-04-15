package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.utils.TestReportDao;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DaoTest extends AbstractDaoConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private ContractMetaDao contractMetaDao;

    @Autowired
    private HikariDataSource dataSource;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private static final int REPORTS_COUNT = 500;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute("truncate rpt.report cascade");
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
        LocalDateTime fromTime = random(LocalDateTime.class);
        LocalDateTime toTime = random(LocalDateTime.class);
        ReportType reportType = random(ReportType.class);
        String timezone = random(TimeZone.class).getID();
        LocalDateTime createdAt = random(LocalDateTime.class);

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
        createReports(currMoment);

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
        createReports(currMoment);

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
    public void severalInstancesReportServiceTest() throws DaoException, ExecutionException, InterruptedException {
        TestReportDao testReportDao = new TestReportDao(dataSource);

        for (int i = 0; i < REPORTS_COUNT; i++) {
            testReportDao.createPendingReport(random(String.class), random(String.class), LocalDateTime.now(),
                    LocalDateTime.now(), ReportType.payment_registry, "UTC", LocalDateTime.now());
        }

        Future<List<Report>> firstFuture = executor.submit(() -> reportDao.getPendingReports(REPORTS_COUNT));
        Future<List<Report>> secondFuture = executor.submit(() -> reportDao.getPendingReports(REPORTS_COUNT));
        List<Report> firstReportList = firstFuture.get();
        List<Report> secondReportList = secondFuture.get();

        boolean isError = firstReportList == null || firstReportList.isEmpty()
                || secondReportList == null || secondReportList.isEmpty();
        assertFalse("One of the report lists is empty", isError);
    }

    private void createReports(LocalDateTime currMoment) {
        IntStream.rangeClosed(1, 15).forEach(i -> {
            try {
                reportDao.createReport("partyId", "shopId", currMoment.minusSeconds(i), currMoment.plusSeconds(i),
                        random(ReportType.class), random(TimeZone.class).getID(), currMoment.plusSeconds(i));
            } catch (DaoException e) {
                throw new RuntimeException();
            }
        });
    }

}
