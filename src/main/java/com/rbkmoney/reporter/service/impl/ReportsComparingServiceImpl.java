package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.reporter.dao.ReportComparingDataDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.handler.comparing.ReportComparingHandler;
import com.rbkmoney.reporter.service.ReportsComparingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "report.local.test.comparing.enabled", havingValue = "true")
public class ReportsComparingServiceImpl implements ReportsComparingService {

    private final ReportComparingDataDao reportComparingDataDao;
    private final ReportComparingHandler paymentRegistryReportComparingHandler;
    private final ReportComparingHandler provisionOfServiceReportComparingHandler;

    @Override
    @Scheduled(fixedDelayString = "${report.local.test.comparing.timeout}")
    public void compareReports() {
        Optional<Report> currentReport = reportComparingDataDao.getNextComparingReport();
        if (currentReport.isEmpty()) {
            return;
        }
        Report report = currentReport.get();
        paymentRegistryReportComparingHandler.compareReport(report);
        if (report.getType() == ReportType.provision_of_service) {
            provisionOfServiceReportComparingHandler.compareReport(report);
        }
        log.info("Comparing report {} was finished", report);
    }

}
