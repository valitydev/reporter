package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class TaskService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ReportService reportService;

    @Autowired
    private StatisticService statisticService;

    @Value("${scheduler.timezone}")
    private ZoneId zoneId;

    @Scheduled(cron = "${scheduler.cron}")
    public void generateProvisionOfServiceReports() {
        YearMonth yearMonth = YearMonth.now();
        Instant fromTime = yearMonth.minusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
        Instant toTime = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant();

        statisticService.getShopAccountings(fromTime, toTime)
                .parallelStream()
                .forEach(shopAccountingModel -> reportService.createReport(
                        shopAccountingModel.getMerchantId(),
                        shopAccountingModel.getShopId(),
                        fromTime,
                        toTime,
                        ReportType.provision_of_service,
                        zoneId,
                        Instant.now()
                ));
    }

    @Scheduled(fixedDelay = 500)
    public void processPendingReports() {
        List<Report> reports = reportService.getPendingReports();
        log.debug("Trying to process {} pending reports", reports.size());
        for (Report report : reports) {
            reportService.generateReport(report);
        }
    }

}
