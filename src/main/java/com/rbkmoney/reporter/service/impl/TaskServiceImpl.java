package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.handler.report.ReportGeneratorHandler;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.service.ScheduleReports;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements ScheduleReports {

    private final ReportService reportService;

    private final ExecutorService reportsThreadPool;

    @Override
    @Scheduled(fixedDelay = 5000)
    public void processPendingReports() {
        log.info("Processing pending reports get started");

        try {
            List<Report> reports = reportService.getPendingReports();
            log.debug("Trying to process {} pending reports", reports.size());

            List<Callable<Long>> callables = reports.stream()
                    .map(this::transformReportToCallable)
                    .collect(Collectors.toList());
            reportsThreadPool.invokeAll(callables);
        } catch (InterruptedException ex) {
            log.error("Received InterruptedException while thread executed report", ex);
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error("Received exception while scheduler processed pending reports", ex);
        }

        log.info("Pending reports were processed");
    }

    private Callable<Long> transformReportToCallable(Report report) {
        return () -> new ReportGeneratorHandler(reportService).handle(report);
    }

}
