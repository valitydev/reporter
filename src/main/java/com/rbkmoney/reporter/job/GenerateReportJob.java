package com.rbkmoney.reporter.job;

import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.exception.StorageException;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.trigger.FreezeTimeCronTrigger;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

import static com.rbkmoney.geck.common.util.TypeUtil.toLocalDateTime;

@Component
public class GenerateReportJob implements Job {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String PARTY_ID = "party_id";

    public static final String CONTRACT_ID = "contract_id";

    public static final String REPORT_TYPE = "report_type";

    @Autowired
    private ReportService reportService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        FreezeTimeCronTrigger trigger = (FreezeTimeCronTrigger) jobExecutionContext.getTrigger();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        String partyId = jobDataMap.getString(PARTY_ID);
        String contractId = jobDataMap.getString(CONTRACT_ID);
        ReportType reportType = TypeUtil.toEnumField(jobDataMap.getString(REPORT_TYPE), ReportType.class);

        log.info("Trying to create report for contract, partyId='{}', contractId='{}', trigger='{}', jobExecutionContext='{}'",
                partyId, contractId, trigger, jobExecutionContext);
        try {
            Instant toTime = trigger.getCurrentCronTime().toInstant();
            ZoneId zoneId = trigger.getTimeZone().toZoneId();
            Instant fromTime = YearMonth.from(toLocalDateTime(toTime, zoneId)).minusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();

            reportService.createReport(partyId, contractId, fromTime, toTime, reportType, zoneId, jobExecutionContext.getFireTime().toInstant());

            log.info("Report for contract have been successfully created, partyId='{}', contractId='{}', trigger='{}', jobExecutionContext='{}'",
                    partyId, contractId, trigger, jobExecutionContext);
        } catch (StorageException | WRuntimeException ex) {
            throw new JobExecutionException(String.format("Job execution failed (partyId='%s', contractId='%s', trigger='%s', jobExecutionContext='%s'), retry",
                    partyId, contractId, trigger, jobExecutionContext), ex, true);
        } catch (Exception ex) {
            JobExecutionException jobExecutionException = new JobExecutionException(
                    String.format("Job execution failed (partyId='%s', contractId='%s', trigger='%s', jobExecutionContext='%s'), stop triggers",
                            partyId, contractId, trigger, jobExecutionContext), ex);
            jobExecutionException.setUnscheduleAllTriggers(true);
            throw jobExecutionException;
        }
    }
}
