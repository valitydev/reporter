package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportDao {

    Report getReport(long reportId) throws DaoException;

    Report getReportDoUpdate(long reportId) throws DaoException;

    Report getReportDoUpdateSkipLocked(long reportId) throws DaoException;

    List<FileMeta> getReportFiles(long reportId) throws DaoException;

    void changeReportStatus(long reportId, ReportStatus status) throws DaoException;

    FileMeta getFile(String fileId) throws DaoException;

    String attachFile(long reportId, FileMeta file) throws DaoException;

    List<Report> getPendingReports(int limit) throws DaoException;

    List<Report> getPendingReportsByType(ReportType reportType) throws DaoException;

    List<Report> getReportsByRange(String partyId, String shopId, List<ReportType> reportTypes,
                                   LocalDateTime fromTime, LocalDateTime toTime) throws DaoException;

    List<Report> getReportsWithToken(String partyId, String shopId, List<ReportType> reportTypes, LocalDateTime fromTime,
                                     LocalDateTime toTime, LocalDateTime createdAfter, int limit) throws DaoException;

    long createReport(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime,
                      ReportType reportType, String timezone, LocalDateTime createdAt) throws DaoException;
}
