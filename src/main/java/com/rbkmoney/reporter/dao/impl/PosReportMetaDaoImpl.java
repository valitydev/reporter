package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractGenericDao;
import com.rbkmoney.reporter.dao.PosReportMetaDao;
import com.rbkmoney.reporter.domain.tables.pojos.PosReportMeta;
import com.rbkmoney.reporter.exception.DaoException;
import org.jooq.Query;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static com.rbkmoney.reporter.domain.Tables.POS_REPORT_META;

@Component
public class PosReportMetaDaoImpl extends AbstractGenericDao implements PosReportMetaDao {

    private final RowMapper<PosReportMeta> rowMapper;

    public PosReportMetaDaoImpl(DataSource dataSource) {
        super(dataSource);
        rowMapper = BeanPropertyRowMapper.newInstance(PosReportMeta.class);
    }

    @Override
    public PosReportMeta get(String partyId, String contractId) throws DaoException {
        Query query = getDslContext().selectFrom(POS_REPORT_META).where(
                POS_REPORT_META.PARTY_ID.eq(partyId)
                        .and(POS_REPORT_META.CONTRACT_ID.eq(contractId))
        );

        return fetchOne(query, rowMapper);
    }

    @Override
    public void updateLastBalanceAndCreatedAt(String partyId, String contractId, long openingBalance, long closingBalance, LocalDateTime reportCreatedAt) throws DaoException {
        Query query = getDslContext().insertInto(POS_REPORT_META)
                .set(POS_REPORT_META.PARTY_ID, partyId)
                .set(POS_REPORT_META.CONTRACT_ID, contractId)
                .set(POS_REPORT_META.LAST_OPENING_BALANCE, openingBalance)
                .set(POS_REPORT_META.LAST_CLOSING_BALANCE, closingBalance)
                .set(POS_REPORT_META.LAST_REPORT_CREATED_AT, reportCreatedAt)
                .onDuplicateKeyUpdate()
                .set(POS_REPORT_META.LAST_OPENING_BALANCE, openingBalance)
                .set(POS_REPORT_META.LAST_CLOSING_BALANCE, closingBalance)
                .set(POS_REPORT_META.LAST_REPORT_CREATED_AT, reportCreatedAt);

        executeOne(query);
    }
}
