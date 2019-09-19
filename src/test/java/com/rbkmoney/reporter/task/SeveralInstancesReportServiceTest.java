package com.rbkmoney.reporter.task;

import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.dao.impl.ReportDaoImpl;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.task.dao.TestReportDao;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertFalse;

public class SeveralInstancesReportServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ReportDaoImpl reportDao;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private ReportService reportService;

    @MockBean
    private EventPublisher eventPublisher;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private static final int REPORTS_COUNT = 5000;

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

}
