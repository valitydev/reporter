package com.rbkmoney.reporter.utils;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import org.jooq.Query;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static com.rbkmoney.reporter.domain.tables.Report.REPORT;

public class TestReportDao extends AbstractGenericDao {

    private final RowMapper<Report> reportRowMapper;

    public TestReportDao(DataSource dataSource) {
        super(dataSource);
        reportRowMapper = BeanPropertyRowMapper.newInstance(Report.class);
    }

    public long createPendingReport(String partyId,
                                    String shopId,
                                    LocalDateTime fromTime,
                                    LocalDateTime toTime,
                                    ReportType reportType,
                                    String timezone,
                                    LocalDateTime createdAt) throws DaoException {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        Query query = getDslContext().insertInto(REPORT)
                .set(REPORT.PARTY_ID, partyId)
                .set(REPORT.PARTY_SHOP_ID, shopId)
                .set(REPORT.FROM_TIME, fromTime)
                .set(REPORT.TO_TIME, toTime)
                .set(REPORT.TYPE, reportType)
                .set(REPORT.TIMEZONE, timezone)
                .set(REPORT.CREATED_AT, createdAt)
                .set(REPORT.STATUS, ReportStatus.pending)
                .returning(REPORT.ID);

        execute(query, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
