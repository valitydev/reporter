package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.reporter.dao.ContractMetaDao;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.exception.DaoException;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.ContractMeta.CONTRACT_META;

@Component
public class ContractMetaDaoImpl extends AbstractGenericDao implements ContractMetaDao {

    private final RowMapper<ContractMeta> contractMetaRowMapper;

    @Autowired
    public ContractMetaDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        contractMetaRowMapper = BeanPropertyRowMapper.newInstance(ContractMeta.class);
    }

    @Override
    public Long getLastEventId() throws DaoException {
        Query query = getDslContext().select(DSL.max(CONTRACT_META.LAST_EVENT_ID)).from(CONTRACT_META);
        return fetchOne(query, Long.class);
    }

    @Override
    public void save(ContractMeta contractMeta) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().insertInto(CONTRACT_META)
                .set(getDslContext().newRecord(CONTRACT_META, contractMeta))
                .set(CONTRACT_META.WTIME, now)
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(CONTRACT_META, contractMeta))
                .set(CONTRACT_META.WTIME, now);
        executeOne(query);
    }

    @Override
    public ContractMeta get(String partyId, String contractId) throws DaoException {
        Query query = getDslContext().selectFrom(CONTRACT_META)
                .where(CONTRACT_META.PARTY_ID.eq(partyId)
                        .and(CONTRACT_META.CONTRACT_ID.eq(contractId)));
        return fetchOne(query, contractMetaRowMapper);
    }

    @Override
    public ContractMeta getExclusive(String partyId, String contractId) throws DaoException {
        Query query = getDslContext().selectFrom(CONTRACT_META)
                .where(CONTRACT_META.PARTY_ID.eq(partyId)
                        .and(CONTRACT_META.CONTRACT_ID.eq(contractId)))
                .forUpdate();

        return fetchOne(query, contractMetaRowMapper);
    }

    @Override
    public List<ContractMeta> getByCalendarAndSchedulerId(int calendarId, int schedulerId) throws DaoException {
        Query query = getDslContext().selectFrom(CONTRACT_META)
                .where(
                        CONTRACT_META.CALENDAR_ID.eq(calendarId)
                                .and(CONTRACT_META.SCHEDULE_ID.eq(schedulerId))
                );

        return fetch(query, contractMetaRowMapper);
    }

    @Override
    public List<ContractMeta> getAllActiveContracts() throws DaoException {
        Query query = getDslContext().selectFrom(CONTRACT_META)
                .where(CONTRACT_META.SCHEDULE_ID.isNotNull()
                        .and(CONTRACT_META.CALENDAR_ID.isNotNull()))
                .forUpdate()
                .skipLocked();

        return fetch(query, contractMetaRowMapper);
    }

    @Override
    public void saveLastClosingBalance(String partyId, String contractId, long lastClosingBalance) throws DaoException {
        Query query = getDslContext()
                .update(CONTRACT_META)
                .set(CONTRACT_META.LAST_CLOSING_BALANCE, lastClosingBalance)
                .where(CONTRACT_META.PARTY_ID.eq(partyId)
                        .and(CONTRACT_META.CONTRACT_ID.eq(contractId)));

        executeOne(query);
    }

    @Override
    public void disableContract(String partyId, String contractId) throws DaoException {
        Query query = getDslContext().update(CONTRACT_META)
                .set(CONTRACT_META.CALENDAR_ID, (Integer) null)
                .set(CONTRACT_META.SCHEDULE_ID, (Integer) null)
                .where(CONTRACT_META.PARTY_ID.eq(partyId)
                        .and(CONTRACT_META.CONTRACT_ID.eq(contractId)));

        executeOne(query);
    }

    @Override
    public void updateLastReportCreatedAt(String partyId, String contractId, LocalDateTime reportCreatedAt)
            throws DaoException {
        Query query = getDslContext()
                .update(CONTRACT_META)
                .set(CONTRACT_META.LAST_REPORT_CREATED_AT, reportCreatedAt)
                .where(
                        CONTRACT_META.PARTY_ID.eq(partyId)
                                .and(CONTRACT_META.CONTRACT_ID.eq(contractId))
                );
        executeOne(query);
    }
}
