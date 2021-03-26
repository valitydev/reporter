package com.rbkmoney.reporter.job;

import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.exception.StorageException;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.trigger.FreezeTimeCronTriggerImpl;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static com.rbkmoney.geck.common.util.TypeUtil.toLocalDateTime;

@Slf4j
@Component
public class GenerateReportJob implements Job {

    public static final String PARTY_ID = "party_id";

    public static final String CONTRACT_ID = "contract_id";

    public static final String REPORT_TYPE = "report_type";

    @Autowired
    private ReportService reportService;

    @Autowired
    private PartyService partyService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        FreezeTimeCronTriggerImpl trigger = (FreezeTimeCronTriggerImpl) jobExecutionContext.getTrigger();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        String partyId = jobDataMap.getString(PARTY_ID);
        String contractId = jobDataMap.getString(CONTRACT_ID);
        ReportType reportType = TypeUtil.toEnumField(jobDataMap.getString(REPORT_TYPE), ReportType.class);

        log.info("Trying to create report for contract, partyId='{}', " +
                        "contractId='{}', trigger='{}', jobExecutionContext='{}'",
                partyId, contractId, trigger, jobExecutionContext);
        try {
            Instant toTime = trigger.getCurrentCronTime().toInstant();
            ZoneId zoneId = trigger.getTimeZone().toZoneId();
            Instant fromTime =
                    YearMonth.from(toLocalDateTime(toTime, zoneId)).minusMonths(1).atDay(1).atStartOfDay(zoneId)
                            .toInstant();

            List<Shop> shops = partyService.getParty(partyId).getShops().values()
                    .stream().filter(shop -> shop.getContractId().equals(contractId))
                    .collect(Collectors.toList());

            if (shops.isEmpty()) {
                log.info("No shops found, partyId='{}', contractId='{}', trigger='{}', jobExecutionContext='{}'",
                        partyId, contractId, trigger, jobExecutionContext);
                return;
            }

            shops.forEach(shop -> reportService
                    .createReport(partyId, shop.getId(), fromTime, toTime, reportType, zoneId,
                            jobExecutionContext.getFireTime().toInstant()));

            log.info("Report for contract have been successfully created, partyId='{}', " +
                            "contractId='{}', trigger='{}', jobExecutionContext='{}'",
                    partyId, contractId, trigger, jobExecutionContext);
        } catch (StorageException | WRuntimeException ex) {
            throw new JobExecutionException(String.format(
                    "Job execution failed (partyId='%s', contractId='%s', " +
                            "trigger='%s', jobExecutionContext='%s'), retry",
                    partyId, contractId, trigger, jobExecutionContext), ex, true);
        } catch (Exception ex) {
            JobExecutionException jobExecutionException = new JobExecutionException(
                    String.format(
                            "Job execution failed (partyId='%s', contractId='%s', " +
                                    "trigger='%s', jobExecutionContext='%s'), stop triggers",
                            partyId, contractId, trigger, jobExecutionContext), ex);
            jobExecutionException.setUnscheduleAllTriggers(true);
            throw jobExecutionException;
        }
    }
}
