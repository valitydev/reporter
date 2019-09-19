package com.rbkmoney.reporter.handler;

import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.service.ReportService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReportGeneratorHandler implements Handler<Report, Long> {

    private final ReportService reportService;

    @Override
    public Long handle(Report report) {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        currentThread.setName("report-" + report.getId());
        try {
            reportService.generateReport(report);
            return report.getId();
        } finally {
            currentThread.setName(oldName);
        }
    }

}
