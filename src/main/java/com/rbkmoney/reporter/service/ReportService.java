package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.reporter.dao.ContractMetaDao;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

/**
 * Created by tolkonepiu on 17/07/2017.
 */
@Service
public class ReportService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ReportDao reportDao;

    private final PartyService partyService;

    private final List<TemplateService> templateServices;

    private final StorageService storageService;

    private final SignService signService;

    private final ContractMetaDao contractMetaDao;

    private final ZoneId defaultTimeZone;

    @Autowired
    public ReportService(
            ReportDao reportDao,
            PartyService partyService,
            List<TemplateService> templateServices,
            StorageService storageService,
            SignService signService,
            ContractMetaDao contractMetaDao,
            @Value("${report.defaultTimeZone}") ZoneId defaultTimeZone
    ) {
        this.reportDao = reportDao;
        this.partyService = partyService;
        this.templateServices = templateServices;
        this.storageService = storageService;
        this.signService = signService;
        this.contractMetaDao = contractMetaDao;
        this.defaultTimeZone = defaultTimeZone;
    }

    public List<Report> getReportsByRange(String partyId, String shopId, List<ReportType> reportTypes, Instant fromTime, Instant toTime) throws StorageException {
        try {
            return reportDao.getReportsByRange(
                    partyId,
                    shopId,
                    reportTypes,
                    LocalDateTime.ofInstant(fromTime, ZoneOffset.UTC),
                    LocalDateTime.ofInstant(toTime, ZoneOffset.UTC)
            );
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get reports by range, partyId='%s', shopId='%s', reportTypes='%s', fromTime='%s', toTime='%s'",
                    partyId, shopId, reportTypes, fromTime, toTime), ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public List<Report> getPendingReports() throws StorageException {
        try {
            return reportDao.getPendingReports();
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

    public Report getReport(String partyId, String shopId, long reportId) throws ReportNotFoundException, StorageException {
        try {
            Report report = reportDao.getReport(partyId, shopId, reportId);
            if (report == null) {
                throw new ReportNotFoundException(String.format("Report not found, partyId='%s', shopId='%s', reportId='%d'", partyId, shopId, reportId));
            }
            return report;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get report from storage, partyId='%s', shopId='%s', reportId='%d'", partyId, shopId, reportId), ex);
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

    @Transactional(propagation = Propagation.REQUIRED)
    public void generateReport(Report report) {
        log.info("Trying to process report, reportId='{}', reportType='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                report.getId(), report.getType(), report.getPartyId(), report.getPartyShopId(), report.getFromTime(), report.getToTime());
        try {
            Shop shop = partyService.getShop(report.getPartyId(), report.getPartyShopId());
            String contractId = shop.getContractId();
            ContractMeta contractMeta = contractMetaDao.getExclusive(report.getPartyId(), contractId, report.getType());
            if (contractMeta == null) {
                throw new NotFoundException(String.format("Failed to find meta data for contract, partyId='%s', contractId='%s', reportType='%s'",
                        report.getPartyId(), contractId, report.getType()));
            }
            List<FileMeta> reportFiles = processSignAndUpload(report, contractMeta);
            finishedReportTask(report.getId(), reportFiles);
            contractMetaDao.updateLastReportCreatedAt(report.getPartyId(), contractId, report.getType(), report.getCreatedAt());
            log.info("Report has been successfully processed, reportId='{}', reportType='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                    report.getId(), report.getType(), report.getPartyId(), report.getPartyShopId(), report.getFromTime(), report.getToTime());
        } catch (ValidationException ex) {
            log.error("Report data validation failed, reportId='{}'", report.getId(), ex);
            changeReportStatus(report, ReportStatus.cancelled);
        } catch (Throwable throwable) {
            log.error("The report has failed to process, reportId='{}', reportType='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                    report.getId(), report.getType(), report.getPartyId(), report.getPartyShopId(), report.getFromTime(), report.getToTime(), throwable);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelReport(String partyId, String shopId, long reportId) throws ReportNotFoundException, StorageException {
        log.info("Trying to cancel report, reportId='{}'", reportId);
        Report report = getReport(partyId, shopId, reportId);
        changeReportStatus(report, ReportStatus.cancelled);
        log.info("Report have been cancelled, reportId='{}'", reportId);
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
            throw new StorageException(String.format("Failed to finish report task, reportId='%d'", reportId));
        }
    }

    public List<FileMeta> processSignAndUpload(Report report, ContractMeta contractMeta) throws IOException {
        boolean needSign = partyService.needSign(report.getPartyId(), contractMeta.getContractId());
        List<FileMeta> files = new ArrayList<>();
        for (TemplateService templateService : templateServices) {
            if (templateService.accept(report.getType(), contractMeta)) {
                Path reportFile = Files.createTempFile(report.getType() + "_", "_report.xlsx");
                try {
                    templateService.processReportTemplate(
                            report,
                            contractMeta,
                            Files.newOutputStream(reportFile)
                    );
                    FileMeta reportFileModel = storageService.saveFile(reportFile);
                    files.add(reportFileModel);

                    if (needSign) {
                        byte[] sign = signService.sign(reportFile);
                        FileMeta signFileModel = storageService.saveFile(reportFile.getFileName().toString() + ".sgn", sign);
                        files.add(signFileModel);
                    }
                } finally {
                    Files.deleteIfExists(reportFile);
                }
            }
        }
        return files;
    }

}
