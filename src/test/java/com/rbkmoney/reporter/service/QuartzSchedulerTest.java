package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.job.GenerateReportJob;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.impl.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.service.impl.ProvisionOfServiceTemplateImpl;
import com.rbkmoney.reporter.trigger.FreezeTimeCronScheduleBuilder;
import com.rbkmoney.reporter.trigger.FreezeTimeCronTrigger;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.junit.Test;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import static com.rbkmoney.reporter.util.QuartzJobUtil.buildJobKey;
import static com.rbkmoney.reporter.util.QuartzJobUtil.buildTriggerKey;
import static com.rbkmoney.reporter.util.TestDataUtil.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzSchedulerTest extends AbstractIntegrationTest {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ReportDao reportDao;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private SignService signService;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private PartyService partyService;

    @MockBean
    private PaymentRegistryTemplateImpl paymentRegistryTemplate;

    @MockBean
    private ProvisionOfServiceTemplateImpl provisionOfServiceTemplate;

    @MockBean
    private RepositoryClientSrv.Iface dominantClient;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

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
        String partyId = "TestPartyID";
        String shopId = "TestShopID";
        String contractId = random(String.class);
        Party testParty = getTestParty(partyId, shopId, contractId);

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(testParty);
        given(partyManagementClient.getMetaData(any(), any(), any()))
                .willReturn(Value.b(true));
        ShopAccountingModel shopAccountingModel = random(ShopAccountingModel.class);
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(Instant.class)))
                .willReturn(shopAccountingModel);
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(), any(Instant.class)))
                .willReturn(shopAccountingModel);
        given(dominantClient.checkoutObject(any(), eq(Reference.payment_institution(new PaymentInstitutionRef(1)))))
                .willReturn(buildPaymentInstitutionObject(new PaymentInstitutionRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.calendar(new CalendarRef(1)))))
                .willReturn(buildPaymentCalendarObject(new CalendarRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.business_schedule(new BusinessScheduleRef(1)))))
                .willReturn(buildPayoutScheduleObject(new BusinessScheduleRef(1)));
        when(partyService.getParty(any(String.class))).thenReturn(testParty);

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
