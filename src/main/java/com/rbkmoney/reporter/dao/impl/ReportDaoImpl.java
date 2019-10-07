package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractGenericDao;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Condition;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.FileMeta.FILE_META;
import static com.rbkmoney.reporter.domain.tables.Report.REPORT;
import static java.util.Optional.ofNullable;
import static org.jooq.impl.DSL.trueCondition;

@Component
public class ReportDaoImpl extends AbstractGenericDao implements ReportDao {

    private final RowMapper<Report> reportRowMapper;

    private final RowMapper<FileMeta> fileMetaRowMapper;

    @Autowired
    public ReportDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        reportRowMapper = BeanPropertyRowMapper.newInstance(Report.class);
        fileMetaRowMapper = BeanPropertyRowMapper.newInstance(FileMeta.class);
    }

    @Override
    public Report getReport(String partyId, String shopId, long reportId) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT).where(
                REPORT.ID.eq(reportId)
                        .and(REPORT.PARTY_ID.eq(partyId))
                        .and(REPORT.PARTY_SHOP_ID.eq(shopId))
        );
        return fetchOne(query, reportRowMapper);
    }

    @Override
    public Report getReportDoUpdate(String partyId, String shopId, long reportId) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT).where(
                REPORT.ID.eq(reportId)
                        .and(REPORT.PARTY_ID.eq(partyId))
                        .and(REPORT.PARTY_SHOP_ID.eq(shopId)))
                .forUpdate();
        return fetchOne(query, reportRowMapper);
    }

    @Override
    public Report getReport(long reportId) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT).where(REPORT.ID.eq(reportId));
        return fetchOne(query, reportRowMapper);
    }

    @Override
    public Report getReportDoUpdate(long reportId) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT).where(REPORT.ID.eq(reportId))
                .forUpdate();
        return fetchOne(query, reportRowMapper);
    }

    @Override
    public Report getReportDoUpdateSkipLocked(String partyId, String shopId, long reportId) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT).where(
                REPORT.ID.eq(reportId)
                        .and(REPORT.PARTY_ID.eq(partyId))
                        .and(REPORT.PARTY_SHOP_ID.eq(shopId)))
                .forUpdate()
                .skipLocked();
        return fetchOne(query, reportRowMapper);
    }

    @Override
    public List<FileMeta> getReportFiles(long reportId) throws DaoException {
        Query query = getDslContext().selectFrom(FILE_META)
                .where(
                        FILE_META.REPORT_ID.eq(reportId)
                );
        return fetch(query, fileMetaRowMapper);

    }

    @Override
    public void changeReportStatus(long reportId, ReportStatus status) throws DaoException {
        Query query = getDslContext().update(REPORT)
                .set(REPORT.STATUS, status)
                .where(REPORT.ID.eq(reportId));

        executeOne(query);
    }

    @Override
    public FileMeta getFile(String fileId) throws DaoException {
        Query query = getDslContext()
                .selectFrom(FILE_META)
                .where(FILE_META.FILE_ID.eq(fileId));

        return fetchOne(query, fileMetaRowMapper);
    }

    @Override
    public String attachFile(long reportId, FileMeta file) throws DaoException {
        Query query = getDslContext().insertInto(FILE_META)
                .set(FILE_META.FILE_ID, file.getFileId())
                .set(FILE_META.REPORT_ID, reportId)
                .set(FILE_META.BUCKET_ID, file.getBucketId())
                .set(FILE_META.FILENAME, file.getFilename())
                .set(FILE_META.MD5, file.getMd5())
                .set(FILE_META.SHA256, file.getSha256());
        executeOne(query);

        return file.getFileId();
    }

    @Override
    public List<Report> getPendingReports(int limit) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT)
                .where(REPORT.STATUS.eq(ReportStatus.pending))
                .limit(limit)
                .forUpdate()
                .skipLocked();

        return fetch(query, reportRowMapper);
    }

    @Override
    public List<Report> getPendingReportsByType(ReportType reportType) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT)
                .where(REPORT.STATUS.eq(ReportStatus.pending))
                .and(REPORT.TYPE.eq(reportType))
                .forUpdate()
                .skipLocked();

        return fetch(query, reportRowMapper);
    }

    @Override
    public List<Report> getReportsByRange(String partyId, String shopId, List<ReportType> reportTypes, LocalDateTime fromTime, LocalDateTime toTime) throws DaoException {
        Condition condition = REPORT.PARTY_ID.eq(partyId)
                .and(ofNullable(shopId).map(REPORT.PARTY_SHOP_ID::eq).orElse(trueCondition()))
                .and(REPORT.FROM_TIME.ge(fromTime))
                .and(REPORT.TO_TIME.le(toTime));

        if (!reportTypes.isEmpty()) {
            condition = condition.and(REPORT.TYPE.in(reportTypes));
        }

        Query query = getDslContext().selectFrom(REPORT).where(condition);

        return fetch(query, reportRowMapper);
    }

    @Override
    public long createReport(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, ReportType reportType, String timezone, LocalDateTime createdAt) throws DaoException {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        Query query = getDslContext().insertInto(REPORT)
                .set(REPORT.PARTY_ID, partyId)
                .set(REPORT.PARTY_SHOP_ID, shopId)
                .set(REPORT.FROM_TIME, fromTime)
                .set(REPORT.TO_TIME, toTime)
                .set(REPORT.TYPE, reportType)
                .set(REPORT.TIMEZONE, timezone)
                .set(REPORT.CREATED_AT, createdAt)
                .returning(REPORT.ID);

        executeOneWithReturn(query, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
