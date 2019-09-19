package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.damsel.base.TimeSpan;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain.Calendar;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.dao.ContractMetaDao;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.exception.NotFoundException;
import com.rbkmoney.reporter.exception.ScheduleProcessingException;
import com.rbkmoney.reporter.exception.StorageException;
import com.rbkmoney.reporter.handler.ReportGeneratorHandler;
import com.rbkmoney.reporter.job.GenerateReportJob;
import com.rbkmoney.reporter.service.*;
import com.rbkmoney.reporter.trigger.FreezeTimeCronScheduleBuilder;
import com.rbkmoney.reporter.util.SchedulerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.calendar.HolidayCalendar;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService, ScheduleReports {

    private final Scheduler scheduler;

    private final ContractMetaDao contractMetaDao;

    private final ReportService reportService;

    private final PartyService partyService;

    private final DomainConfigService domainConfigService;

    private final ExecutorService reportsThreadPool;

    @Override
    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public void syncJobs() {
        try {
            log.info("Starting synchronization of jobs...");
            List<ContractMeta> activeContracts = contractMetaDao.getAllActiveContracts();
            if (activeContracts.isEmpty()) {
                log.info("No active contracts found, nothing to do");
                return;
            }

            for (ContractMeta contractMeta : activeContracts) {
                JobKey jobKey = buildJobKey(
                        contractMeta.getPartyId(),
                        contractMeta.getContractId(),
                        contractMeta.getCalendarId(),
                        contractMeta.getScheduleId()
                );
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (triggers.isEmpty() || !triggers.stream().allMatch(this::isTriggerOnNormalState)) {
                    if (scheduler.checkExists(jobKey)) {
                        log.warn("Inactive job found, please check it manually. Job will be restored, contractMeta='{}'", contractMeta);
                    }
                    createJob(
                            contractMeta.getPartyId(),
                            contractMeta.getContractId(),
                            new CalendarRef(contractMeta.getCalendarId()),
                            new BusinessScheduleRef(contractMeta.getScheduleId())
                    );
                }
            }
        } catch (DaoException | SchedulerException ex) {
            throw new ScheduleProcessingException("Failed to sync jobs", ex);
        } finally {
            log.info("End synchronization of jobs");
        }
    }

    private boolean isTriggerOnNormalState(Trigger trigger) {
        try {
            Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
            log.debug("Trigger '{}' on '{}' state", trigger, triggerState);
            return triggerState == Trigger.TriggerState.NORMAL;
        } catch (SchedulerException ex) {
            throw new ScheduleProcessingException(String.format("Failed to get trigger state, triggerKey='%s'", trigger.getKey()), ex);
        }
    }

    @Override
    @Transactional
    public void registerProvisionOfServiceJob(String partyId, String contractId, long lastEventId, BusinessScheduleRef scheduleRef, Representative signer) throws ScheduleProcessingException, NotFoundException, StorageException {
        log.info("Trying to register provision of service job, partyId='{}', contractId='{}', scheduleId='{}', signer='{}'",
                partyId, contractId, scheduleRef, signer);
        PaymentInstitutionRef paymentInstitutionRef = partyService.getPaymentInstitutionRef(partyId, contractId);
        PaymentInstitution paymentInstitution = domainConfigService.getPaymentInstitution(paymentInstitutionRef);

        if (!paymentInstitution.isSetCalendar()) {
            throw new NotFoundException(
                    String.format("Calendar not found, partyId='%s', contractId='%s'", partyId, contractId)
            );
        }
        CalendarRef calendarRef = paymentInstitution.getCalendar();

        try {
            ContractMeta contractMeta = new ContractMeta();
            contractMeta.setPartyId(partyId);
            contractMeta.setContractId(contractId);
            contractMeta.setCalendarId(calendarRef.getId());
            contractMeta.setLastEventId(lastEventId);
            contractMeta.setScheduleId(scheduleRef.getId());

            contractMeta.setRepresentativeFullName(signer.getFullName());
            contractMeta.setRepresentativePosition(signer.getPosition());
            contractMeta.setRepresentativeDocument(signer.getDocument().getSetField().getFieldName());
            if (signer.getDocument().isSetPowerOfAttorney()) {
                LegalAgreement legalAgreement = signer.getDocument().getPowerOfAttorney();
                contractMeta.setLegalAgreementId(legalAgreement.getLegalAgreementId());
                contractMeta.setLegalAgreementSignedAt(TypeUtil.stringToLocalDateTime(legalAgreement.getSignedAt()));
                contractMeta.setLegalAgreementValidUntil(TypeUtil.stringToLocalDateTime(legalAgreement.getValidUntil()));
            }

            contractMetaDao.save(contractMeta);
            createJob(partyId, contractId, calendarRef, scheduleRef);
            log.info("Job have been successfully enabled, partyId='{}', contractId='{}', scheduleRef='{}', calendarRef='{}'",
                    partyId, contractId, scheduleRef, calendarRef);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format(
                            "Failed to save job on storage, partyId='%s', contractId='%s', scheduleRef='%s', calendarRef='%s'",
                            partyId, contractId, scheduleRef, calendarRef), ex);
        }
    }

    private void createJob(String partyId, String contractId, CalendarRef calendarRef, BusinessScheduleRef scheduleRef)
            throws ScheduleProcessingException {
        log.info("Trying to create job, partyId='{}', contractId='{}', calendarRef='{}', scheduleRef='{}'",
                partyId, contractId, calendarRef, scheduleRef);
        try {
            BusinessSchedule schedule = domainConfigService.getBusinessSchedule(scheduleRef);
            Calendar calendar = domainConfigService.getCalendar(calendarRef);

            String calendarId = "calendar-" + calendarRef.getId();
            HolidayCalendar holidayCalendar = SchedulerUtil.buildCalendar(calendar);
            scheduler.addCalendar(calendarId, holidayCalendar, true, true);
            log.info("New calendar was saved, calendarRef='{}', calendarId='{}'", calendarRef, calendarId);

            JobDetail jobDetail = JobBuilder.newJob(GenerateReportJob.class)
                    .withIdentity(buildJobKey(partyId, contractId, calendarRef.getId(), scheduleRef.getId()))
                    .withDescription(schedule.getDescription())
                    .usingJobData(GenerateReportJob.PARTY_ID, partyId)
                    .usingJobData(GenerateReportJob.CONTRACT_ID, contractId)
                    .usingJobData(GenerateReportJob.REPORT_TYPE, ReportType.provision_of_service.toString())
                    .build();

            Set<Trigger> triggers = new HashSet<>();
            List<String> cronList = SchedulerUtil.buildCron(schedule.getSchedule());
            for (int triggerId = 0; triggerId < cronList.size(); triggerId++) {
                String cron = cronList.get(triggerId);

                FreezeTimeCronScheduleBuilder freezeTimeCronScheduleBuilder = FreezeTimeCronScheduleBuilder
                        .cronSchedule(cron)
                        .inTimeZone(TimeZone.getTimeZone(calendar.getTimezone()));
                if (schedule.isSetDelay()) {
                    TimeSpan timeSpan = schedule.getDelay();
                    freezeTimeCronScheduleBuilder.withYears(timeSpan.getYears())
                            .withMonths(timeSpan.getMonths())
                            .withDays(timeSpan.getDays())
                            .withHours(timeSpan.getHours())
                            .withMinutes(timeSpan.getMinutes())
                            .withSeconds(timeSpan.getSeconds());
                }

                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(buildTriggerKey(partyId, contractId, calendarRef.getId(), scheduleRef.getId(), triggerId))
                        .withDescription(schedule.getDescription())
                        .forJob(jobDetail)
                        .withSchedule(freezeTimeCronScheduleBuilder)
                        .modifiedByCalendar(calendarId)
                        .build();
                triggers.add(trigger);
            }
            scheduler.scheduleJob(jobDetail, triggers, true);
            log.info("Jobs have been successfully created or updated, partyId='{}', contractId='{}', calendarRef='{}', scheduleRef='{}', jobDetail='{}', triggers='{}'", partyId, contractId, calendarRef, scheduleRef, jobDetail, triggers);
        } catch (NotFoundException | SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to create job, partyId='%s', contractId='%s', calendarRef='%s', scheduleRef='%s'",
                            partyId, contractId, calendarRef, scheduleRef), ex);
        }
    }

    @Override
    @Transactional
    public void deregisterProvisionOfServiceJob(String partyId, String contractId) throws ScheduleProcessingException, StorageException {
        log.info("Trying to deregister provision of service job, partyId='{}', contractId='{}'", partyId, contractId);
        try {
            ContractMeta contractMeta = contractMetaDao.get(partyId, contractId);
            if (contractMeta != null) {
                contractMetaDao.disableContract(partyId, contractId);
                removeJob(contractMeta);
                log.info("Provision of service job have been successfully disabled, partyId='{}', contractId='{}', scheduleId='{}', calendarId='{}'",
                        partyId, contractId, contractMeta.getScheduleId(), contractMeta.getCalendarId());
            } else {
                log.warn("Not possible to disable provision of service job, contract meta not found, partyId='{}', contractId='{}'", partyId, contractId);
            }
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to disable provision of service job on storage, partyId='%s', contractId='%s'",
                            partyId, contractId), ex);
        }
    }

    private void removeJob(ContractMeta contractMeta) {
        try {
            if (contractMeta.getCalendarId() != null && contractMeta.getScheduleId() != null) {
                JobKey jobKey = buildJobKey(
                        contractMeta.getPartyId(),
                        contractMeta.getContractId(),
                        contractMeta.getCalendarId(),
                        contractMeta.getScheduleId()
                );
                List<TriggerKey> triggerKeys = scheduler.getTriggersOfJob(jobKey).stream()
                        .map(trigger -> trigger.getKey())
                        .collect(Collectors.toList());

                scheduler.unscheduleJobs(triggerKeys);
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to disable job, contractMeta='%s'",
                            contractMeta), ex);
        }
    }

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

    private JobKey buildJobKey(String partyId, String contractId, int calendarId, int scheduleId) {
        return JobKey.jobKey(
                String.format("job-%s-%s", partyId, contractId),
                buildGroupKey(calendarId, scheduleId)
        );
    }

    private TriggerKey buildTriggerKey(String partyId, String contractId, int calendarId, int scheduleId, int triggerId) {
        return TriggerKey.triggerKey(
                String.format("trigger-%s-%s-%d", partyId, contractId, triggerId),
                buildGroupKey(calendarId, scheduleId)
        );
    }

    private String buildGroupKey(int calendarId, int scheduleId) {
        return String.format("group-%d-%d", calendarId, scheduleId);
    }
}
