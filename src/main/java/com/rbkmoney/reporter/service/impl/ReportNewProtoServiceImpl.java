package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.*;
import com.rbkmoney.reporter.service.ReportNewProtoService;
import com.rbkmoney.reporter.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportNewProtoServiceImpl implements ReportNewProtoService {

    private final ReportDao reportDao;
    private final ReportService reportService;

    @Override
    public long createReport(String partyId, String shopId, Instant fromTime, Instant toTime, ReportType reportType) throws PartyNotFoundException, ShopNotFoundException {
        return reportService.createReport(partyId, shopId, fromTime, toTime, reportType);
    }

    @Override
    public Report getReport(long reportId, boolean withLock) throws ReportNotFoundException, StorageException {
        try {
            Report report;
            if (withLock) {
                report = reportDao.getReportDoUpdate(reportId);
            } else {
                report = reportDao.getReport(reportId);
            }
            if (report == null) {
                throw new ReportNotFoundException(String.format("Report not found,  reportId='%d'", reportId));
            }
            return report;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get report from storage, reportId='%d'", reportId), ex);
        }
    }

    @Override
    public List<Report> getReportsWithToken(String partyId, String shopId, List<ReportType> reportTypes,
                                            Instant fromTime, Instant toTime, Instant createdAfter, int limit) throws StorageException {
        try {
            return reportDao.getReportsWithToken(
                    partyId,
                    Objects.requireNonNullElse(Collections.singletonList(shopId), Collections.emptyList()),
                    reportTypes,
                    LocalDateTime.ofInstant(fromTime, ZoneOffset.UTC),
                    LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                    createdAfter != null ? LocalDateTime.ofInstant(createdAfter, ZoneOffset.UTC) : null,
                    limit
            );
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get reports with token, " +
                            "partyId='%s', shopId='%s', reportTypes='%s', fromTime='%s', toTime='%s', createdAfter='%s'",
                    partyId, shopId, reportTypes, fromTime, toTime, createdAfter), ex);
        }
    }

    @Override
    public List<Report> getReportsWithToken(String partyId,
                                            List<String> shopIds,
                                            List<ReportType> reportTypes,
                                            Instant fromTime,
                                            Instant toTime,
                                            Instant createdAfter,
                                            int limit) throws StorageException {
        try {
            return reportDao.getReportsWithToken(
                    partyId,
                    shopIds,
                    reportTypes,
                    LocalDateTime.ofInstant(fromTime, ZoneOffset.UTC),
                    LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                    createdAfter != null ? LocalDateTime.ofInstant(createdAfter, ZoneOffset.UTC) : null,
                    limit
            );
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get reports with token, " +
                            "partyId='%s', shopIds='%s', reportTypes='%s', fromTime='%s', toTime='%s', createdAfter='%s'",
                    partyId, shopIds, reportTypes, fromTime, toTime, createdAfter), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
    public void cancelReport(long reportId) throws ReportNotFoundException, StorageException {
        log.info("Trying to cancel report, reportId='{}'", reportId);
        Report report = getReport(reportId, true);
        if (report.getStatus() != ReportStatus.cancelled) {
            reportService.changeReportStatus(report, ReportStatus.cancelled);
            log.info("Report have been cancelled, reportId='{}'", reportId);
        }
    }

    @Override
    public URL generatePresignedUrl(String fileId, Instant expiresIn) throws FileNotFoundException, StorageException {
        return reportService.generatePresignedUrl(fileId, expiresIn);
    }

    @Override
    public List<FileMeta> getReportFiles(long reportId) throws StorageException {
        return reportService.getReportFiles(reportId);
    }
}
