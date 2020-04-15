package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.*;

import java.net.URL;
import java.time.Instant;
import java.util.List;

public interface ReportNewProtoService {

    long createReport(String partyId, String shopId, Instant fromTime, Instant toTime, ReportType reportType) throws PartyNotFoundException, ShopNotFoundException;

    Report getReport(long reportId, boolean withLock);

    List<Report> getReportsWithToken(String partyId,
                                     String shopId,
                                     List<ReportType> reportTypes,
                                     Instant fromTime,
                                     Instant toTime,
                                     Instant createdAfter,
                                     int limit) throws StorageException;

    List<Report> getReportsWithToken(String partyId,
                                     List<String> shopIds,
                                     List<ReportType> reportTypes,
                                     Instant fromTime,
                                     Instant toTime,
                                     Instant createdAfter,
                                     int limit) throws StorageException;

    void cancelReport(long reportId) throws ReportNotFoundException, StorageException;

    URL generatePresignedUrl(String fileId, Instant expiresIn) throws FileNotFoundException, StorageException;

    List<FileMeta> getReportFiles(long reportId) throws StorageException;

}
