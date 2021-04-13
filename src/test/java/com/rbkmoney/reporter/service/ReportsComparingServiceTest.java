package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.dao.ReportComparingDataDao;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.handler.comparing.ReportComparingHandler;
import com.rbkmoney.reporter.service.impl.ReportsComparingServiceImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class ReportsComparingServiceTest extends AbstractDaoConfig {

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private ReportComparingDataDao reportComparingDataDao;

    @MockBean
    private ReportComparingHandler reportComparingHandler;

    @Test
    public void compareReportsTest() throws DaoException {
        reportDao.createReport(
                "party",
                "shop",
                LocalDateTime.now().minusHours(10L),
                LocalDateTime.now(),
                ReportType.provision_of_service,
                "RO",
                LocalDateTime.now()
        );

        ReportsComparingService reportsComparingService = new ReportsComparingServiceImpl(
                reportComparingDataDao, reportComparingHandler, reportComparingHandler
        );
        reportsComparingService.compareReports();
        verify(reportComparingHandler, timeout(5000L).times(2))
                .compareReport(any());
    }

}
