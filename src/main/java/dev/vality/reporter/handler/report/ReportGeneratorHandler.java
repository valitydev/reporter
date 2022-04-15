package dev.vality.reporter.handler.report;

import dev.vality.reporter.domain.tables.pojos.Report;
import dev.vality.reporter.handler.Handler;
import dev.vality.reporter.service.ReportService;
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
