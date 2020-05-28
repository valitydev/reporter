package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.reporter.dao.PayoutDao;
import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutInternationalAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.rbkmoney.reporter.domain.tables.Payout.PAYOUT;
import static com.rbkmoney.reporter.domain.tables.PayoutAccount.PAYOUT_ACCOUNT;
import static com.rbkmoney.reporter.domain.tables.PayoutInternationalAccount.PAYOUT_INTERNATIONAL_ACCOUNT;
import static com.rbkmoney.reporter.domain.tables.PayoutState.PAYOUT_STATE;

@Component
public class PayoutDaoImpl extends AbstractGenericDao implements PayoutDao {

    @Autowired
    public PayoutDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long savePayout(Payout payout) {
        Query query = getDslContext()
                .insertInto(PAYOUT)
                .set(getDslContext().newRecord(PAYOUT, payout))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(PAYOUT, payout))
                .returning(PAYOUT.ID);
        return fetchOne(query, Long.class);
    }

    @Override
    public void savePayoutAccountInfo(PayoutAccount payoutAccount) {
        Query query = getDslContext()
                .insertInto(PAYOUT_ACCOUNT)
                .set(getDslContext().newRecord(PAYOUT_ACCOUNT, payoutAccount))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(PAYOUT_ACCOUNT, payoutAccount));
        executeOne(query);
    }

    @Override
    public void savePayoutInternationalAccountInfo(PayoutInternationalAccount internationalAccount) {
        Query query = getDslContext()
                .insertInto(PAYOUT_INTERNATIONAL_ACCOUNT)
                .set(getDslContext().newRecord(PAYOUT_INTERNATIONAL_ACCOUNT, internationalAccount))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(PAYOUT_INTERNATIONAL_ACCOUNT, internationalAccount));
        executeOne(query);
    }

    @Override
    public Long savePayoutState(PayoutState payoutState) {
        Query query = getDslContext()
                .insertInto(PAYOUT_STATE)
                .set(getDslContext().newRecord(PAYOUT_STATE, payoutState))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(PAYOUT_STATE, payoutState))
                .returning(PAYOUT_STATE.ID);
        return fetchOne(query, Long.class);
    }

    @Override
    public Optional<Long> getLastEventId() {
        Query query = getDslContext().select(DSL.max(PAYOUT_STATE.EVENT_ID)).from(PAYOUT_STATE);
        return Optional.ofNullable(fetchOne(query, Long.class));
    }

}
