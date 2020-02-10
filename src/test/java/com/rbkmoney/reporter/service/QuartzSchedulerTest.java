package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.config.AbstractSchedulerConfig;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.job.GenerateReportJob;
import com.rbkmoney.reporter.trigger.FreezeTimeCronScheduleBuilder;
import com.rbkmoney.reporter.trigger.FreezeTimeCronTrigger;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.junit.Test;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import static com.rbkmoney.reporter.util.QuartzJobUtil.buildJobKey;
import static com.rbkmoney.reporter.util.QuartzJobUtil.buildTriggerKey;
import static org.junit.Assert.assertTrue;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzSchedulerTest extends AbstractSchedulerConfig {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ReportDao reportDao;

    private static AtomicInteger schedulerCounter = new AtomicInteger(0);

    private static final String CRON = "0/2 * * * * ?";

    @Test
    public void simpleSchedulerTest() throws SchedulerException, InterruptedException {
        JobDetail job = newJob(SampleJob.class)
                .withIdentity("job1", "group1")
                .build();
        CronTrigger trigger = newTrigger()
                .withIdentity("a", "t")
                .withSchedule(cronSchedule(CRON).inTimeZone(TimeZone.getDefault()))
                .forJob(job)
                .build();

        scheduler.scheduleJob(job, trigger);

        Thread.sleep(5000);
        assertTrue("The number of trigger runs is less than expected", schedulerCounter.get() >= 2);
    }

    @Test
    public void customSchedulerTest() throws SchedulerException, InterruptedException, TException, DaoException {
        JobDetail jobDetail = JobBuilder.newJob(GenerateReportJob.class)
                .withIdentity(buildJobKey(partyId, contractId, 1, 1))
                .withDescription("description")
                .usingJobData(GenerateReportJob.PARTY_ID, partyId)
                .usingJobData(GenerateReportJob.CONTRACT_ID, contractId)
                .usingJobData(GenerateReportJob.REPORT_TYPE, ReportType.provision_of_service.toString())
                .build();

        FreezeTimeCronScheduleBuilder freezeTimeCronScheduleBuilder = FreezeTimeCronScheduleBuilder
                .cronSchedule(CRON)
                .inTimeZone(TimeZone.getDefault());
        FreezeTimeCronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(buildTriggerKey("partyId", "contractId", 1, 1, 1))
                .withDescription("some descr for scheduler")
                .forJob(jobDetail)
                .withSchedule(freezeTimeCronScheduleBuilder)
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
        Thread.sleep(15000);
        List<Report> reports = reportDao.getPendingReports(100);
        assertTrue("The number of freeze time trigger runs is less than expected",
                (reports == null ? 0 : reports.size()) >= 2);
    }

    @Slf4j
    public static class SampleJob implements Job {

        public void execute(JobExecutionContext context) throws JobExecutionException {
            schedulerCounter.incrementAndGet();
            log.warn("Sample Job Executing {}", schedulerCounter.get());
        }
    }
}
