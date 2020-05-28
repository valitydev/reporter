package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.*;
import com.rbkmoney.reporter.template.ReportTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReportService {

    private final ReportDao reportDao;

    private final List<ReportTemplate> reportTemplates;

    private final StorageService storageService;

    private final ZoneId defaultTimeZone;

    private final int batchSize;

    public ReportService(
            ReportDao reportDao,
            List<ReportTemplate> reportTemplates,
            StorageService storageService,
            @Value("${report.defaultTimeZone}") ZoneId defaultTimeZone,
            @Value("${report.batchSize}") int batchSize
    ) {
        this.reportDao = reportDao;
        this.reportTemplates = reportTemplates;
        this.storageService = storageService;
        this.defaultTimeZone = defaultTimeZone;
        this.batchSize = batchSize;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public List<Report> getPendingReports() throws StorageException {
        try {
            return reportDao.getPendingReports(batchSize);
        } catch (DaoException ex) {
            throw new StorageException("Failed to get pending reports", ex);
        }
    }

    public List<FileMeta> getReportFiles(long reportId) throws StorageException {
        try {
            return reportDao.getReportFiles(reportId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get report files from storage, reportId='%d'", reportId), ex);
        }
    }

    public long createReport(String partyId, String shopId, Instant fromTime, Instant toTime, ReportType reportType) throws PartyNotFoundException, ShopNotFoundException {
        return createReport(partyId, shopId, fromTime, toTime, reportType, defaultTimeZone, Instant.now());
    }

    public long createReport(String partyId, String shopId, Instant fromTime, Instant toTime, ReportType reportType, ZoneId timezone, Instant createdAt) throws PartyNotFoundException, ShopNotFoundException {
        log.info("Trying to create report, partyId={}, shopId={}, reportType={}, fromTime={}, toTime={}",
                partyId, shopId, reportType, fromTime, toTime);

        try {
            long reportId = reportDao.createReport(
                    partyId,
                    shopId,
                    LocalDateTime.ofInstant(fromTime, ZoneOffset.UTC),
                    LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                    reportType,
                    timezone.getId(),
                    LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC)
            );
            log.info("Report has been successfully created, reportId={}, partyId={}, shopId={}, reportType={}, fromTime={}, toTime={}",
                    reportId, partyId, shopId, reportType, fromTime, toTime);
            return reportId;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to save report in storage, partyId='%s', shopId='%s', fromTime='%s', toTime='%s', reportType='%s'",
                    partyId, shopId, fromTime, toTime, reportType), ex);
        }
    }

    public URL generatePresignedUrl(String fileId, Instant expiresIn) throws FileNotFoundException, StorageException {
        FileMeta file;
        try {
            file = reportDao.getFile(fileId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get file meta, fileId='%s'", fileId), ex);
        }

        if (file == null) {
            throw new FileNotFoundException(String.format("File with id '%s' not found", fileId));
        }

        return storageService.getFileUrl(file.getFileId(), file.getBucketId(), expiresIn);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
    public void generateReport(Report report) {
        log.info("Trying to process report, reportId='{}', reportType='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                report.getId(), report.getType(), report.getPartyId(), report.getPartyShopId(), report.getFromTime(), report.getToTime());
        try {
            Report forUpdateReport = reportDao.getReportDoUpdateSkipLocked(report.getId());
            if (forUpdateReport != null && forUpdateReport.getStatus() == ReportStatus.pending) {
                List<FileMeta> reportFiles = processSignAndUpload(report);
                finishedReportTask(report.getId(), reportFiles);
                log.info("Report has been successfully processed, reportId='{}', reportType='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                        report.getId(), report.getType(), report.getPartyId(), report.getPartyShopId(), report.getFromTime(), report.getToTime());
            }
        } catch (ValidationException | NotFoundException ex) {
            log.error("Report data validation failed, reportId='{}'", report.getId(), ex);
            changeReportStatus(report, ReportStatus.cancelled);
        } catch (Throwable throwable) {
            log.error("The report has failed to process, reportId='{}', reportType='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                    report.getId(), report.getType(), report.getPartyId(), report.getPartyShopId(), report.getFromTime(), report.getToTime(), throwable);
        }
    }

    public void changeReportStatus(Report report, ReportStatus reportStatus) {
        log.info("Trying to change report status, reportId='{}', reportStatus='{}'", report.getId(), reportStatus);
        try {
            reportDao.changeReportStatus(report.getId(), reportStatus);
            log.info("Report status have been successfully changed, reportId='{}', reportStatus='{}'", report.getId(), reportStatus);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to change report status, reportId='%d', reportStatus='%s'", report.getId(), reportStatus), ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void finishedReportTask(long reportId, List<FileMeta> reportFiles) throws StorageException {
        try {
            for (FileMeta file : reportFiles) {
                reportDao.attachFile(reportId, file);
            }

            reportDao.changeReportStatus(reportId, ReportStatus.created);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to finish report task, reportId='%d'", reportId), ex);
        }
    }

    public List<FileMeta> processSignAndUpload(Report report) throws IOException {
        List<FileMeta> files = new ArrayList<>();
        for (ReportTemplate reportTemplate : reportTemplates) {
            if (reportTemplate.isAccept(report.getType())) {
                Path reportFile = Files.createTempFile(report.getType() + "_", "_report.xlsx");
                try {
                    reportTemplate.processReportTemplate(report, Files.newOutputStream(reportFile));
                    FileMeta reportFileModel = storageService.saveFile(reportFile);
                    files.add(reportFileModel);
                } finally {
                    Files.deleteIfExists(reportFile);
                }
            }
        }
        return files;
    }

}
