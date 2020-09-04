package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.mapper.RecordRowMapper;
import com.rbkmoney.reporter.dao.PayoutDao;
import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutInternationalAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.rbkmoney.reporter.domain.tables.Payout.PAYOUT;
import static com.rbkmoney.reporter.domain.tables.PayoutAccount.PAYOUT_ACCOUNT;
import static com.rbkmoney.reporter.domain.tables.PayoutInternationalAccount.PAYOUT_INTERNATIONAL_ACCOUNT;
import static com.rbkmoney.reporter.domain.tables.PayoutState.PAYOUT_STATE;

@Component
public class PayoutDaoImpl extends AbstractGenericDao implements PayoutDao {

    private final RowMapper<Payout> payoutRowMapper;
    private final RowMapper<PayoutAccount> payoutAccountRowMapper;
    private final RowMapper<PayoutInternationalAccount> internationalAccountRowMapper;
    private final RowMapper<PayoutState> payoutStateRowMapper;

    @Autowired
    public PayoutDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        payoutRowMapper = new RecordRowMapper<>(PAYOUT, Payout.class);
        payoutAccountRowMapper = new RecordRowMapper<>(PAYOUT_ACCOUNT, PayoutAccount.class);
        internationalAccountRowMapper =
                new RecordRowMapper<>(PAYOUT_INTERNATIONAL_ACCOUNT, PayoutInternationalAccount.class);
        payoutStateRowMapper = new RecordRowMapper<>(PAYOUT_STATE, PayoutState.class);
    }

    @Override
    public Long savePayout(Payout payout) {
        Query query = getDslContext()
                .insertInto(PAYOUT)
                .set(getDslContext().newRecord(PAYOUT, payout))
                .onConflict(PAYOUT.PAYOUT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT, payout))
                .returning(PAYOUT.ID);
        return fetchOne(query, Long.class);
    }

    @Override
    public Payout getPayout(String payoutId) {
        Query query = getDslContext()
                .selectFrom(PAYOUT)
                .where(PAYOUT.PAYOUT_ID.eq(payoutId));
        return fetchOne(query, payoutRowMapper);
    }

    @Override
    public void savePayoutAccountInfo(PayoutAccount payoutAccount) {
        Query query = getDslContext()
                .insertInto(PAYOUT_ACCOUNT)
                .set(getDslContext().newRecord(PAYOUT_ACCOUNT, payoutAccount))
                .onConflict(PAYOUT_ACCOUNT.EXT_PAYOUT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT_ACCOUNT, payoutAccount));
        executeOne(query);
    }

    @Override
    public PayoutAccount getPayoutAccount(Long extPayoutId) {
        Query query = getDslContext()
                .selectFrom(PAYOUT_ACCOUNT)
                .where(PAYOUT_ACCOUNT.EXT_PAYOUT_ID.eq(extPayoutId));
        return fetchOne(query, payoutAccountRowMapper);
    }

    @Override
    public void savePayoutInternationalAccountInfo(PayoutInternationalAccount internationalAccount) {
        Query query = getDslContext()
                .insertInto(PAYOUT_INTERNATIONAL_ACCOUNT)
                .set(getDslContext().newRecord(PAYOUT_INTERNATIONAL_ACCOUNT, internationalAccount))
                .onConflict(PAYOUT_INTERNATIONAL_ACCOUNT.EXT_PAYOUT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT_INTERNATIONAL_ACCOUNT, internationalAccount));
        executeOne(query);
    }

    @Override
    public PayoutInternationalAccount getPayoutInternationalAccount(Long extPayoutId) {
        Query query = getDslContext()
                .selectFrom(PAYOUT_INTERNATIONAL_ACCOUNT)
                .where(PAYOUT_INTERNATIONAL_ACCOUNT.EXT_PAYOUT_ID.eq(extPayoutId));
        return fetchOne(query, internationalAccountRowMapper);
    }

    @Override
    public Long savePayoutState(PayoutState payoutState) {
        Query query = getDslContext()
                .insertInto(PAYOUT_STATE)
                .set(getDslContext().newRecord(PAYOUT_STATE, payoutState))
                .onConflict(PAYOUT_STATE.EXT_PAYOUT_ID, PAYOUT_STATE.EVENT_CREATED_AT, PAYOUT_STATE.STATUS)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT_STATE, payoutState))
                .returning(PAYOUT_STATE.ID);
        return fetchOne(query, Long.class);
    }

    @Override
    public PayoutState getPayoutState(Long extPayoutId) {
        Query query = getDslContext()
                .selectFrom(PAYOUT_STATE)
                .where(PAYOUT_STATE.EXT_PAYOUT_ID.eq(extPayoutId));
        return fetchOne(query, payoutStateRowMapper);
    }

    @Override
    public Optional<Long> getLastEventId() {
        Query query = getDslContext().select(DSL.max(PAYOUT_STATE.EVENT_ID)).from(PAYOUT_STATE);
        return Optional.ofNullable(fetchOne(query, Long.class));
    }

}
