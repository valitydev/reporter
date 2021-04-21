package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.ReportComparingDataDao;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.domain.tables.pojos.ReportComparingData;
import com.rbkmoney.reporter.domain.tables.records.ReportComparingDataRecord;
import com.rbkmoney.reporter.domain.tables.records.ReportRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.rbkmoney.reporter.domain.Tables.REPORT;
import static com.rbkmoney.reporter.domain.tables.ReportComparingData.REPORT_COMPARING_DATA;

@Component
public class ReportComparingDataDaoImpl extends AbstractDao implements ReportComparingDataDao {

    public ReportComparingDataDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long saveReportComparingData(ReportComparingData reportComparingData) {
        return getDslContext()
                .insertInto(REPORT_COMPARING_DATA)
                .set(getDslContext().newRecord(REPORT_COMPARING_DATA, reportComparingData))
                .onConflict(REPORT_COMPARING_DATA.REPORT_ID, REPORT_COMPARING_DATA.REPORT_TYPE)
                .doUpdate()
                .set(getDslContext().newRecord(REPORT_COMPARING_DATA, reportComparingData))
                .returning(REPORT_COMPARING_DATA.REPORT_ID)
                .fetchOne()
                .getReportId();
    }

    @Override
    public ReportComparingData getReportComparingDataByReportId(long reportId) {
        ReportComparingDataRecord record = getDslContext()
                .selectFrom(REPORT_COMPARING_DATA)
                .where(REPORT_COMPARING_DATA.REPORT_ID.eq(reportId))
                .fetchOne();
        return Optional.ofNullable(record)
                .map(rec -> rec.into(ReportComparingData.class))
                .orElse(null);
    }

    @Override
    public Long getLastProcessedReport() {
        ReportComparingDataRecord record = getDslContext()
                .selectFrom(REPORT_COMPARING_DATA)
                .orderBy(REPORT_COMPARING_DATA.REPORT_ID.desc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(record)
                .map(ReportComparingDataRecord::getReportId)
                .orElse(0L);
    }

    @Override
    public Optional<Report> getNextComparingReport() {
        ReportRecord record = getDslContext()
                .selectFrom(REPORT)
                .where(REPORT.ID.greaterThan(getLastProcessedReport()))
                .orderBy(REPORT.ID.asc())
                .limit(1)
                .forUpdate()
                .skipLocked()
                .fetchOne();
        return Optional.ofNullable(record)
                .map(r -> Optional.of(r.into(Report.class)))
                .orElse(Optional.empty());
    }

}
