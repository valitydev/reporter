package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.PayoutDao;
import com.rbkmoney.reporter.domain.enums.PayoutStatus;
import com.rbkmoney.reporter.domain.tables.pojos.*;
import com.rbkmoney.reporter.domain.tables.records.*;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.reporter.domain.tables.Payout.PAYOUT;
import static com.rbkmoney.reporter.domain.tables.PayoutAccount.PAYOUT_ACCOUNT;
import static com.rbkmoney.reporter.domain.tables.PayoutAggsByHour.PAYOUT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.PayoutInternationalAccount.PAYOUT_INTERNATIONAL_ACCOUNT;
import static com.rbkmoney.reporter.domain.tables.PayoutState.PAYOUT_STATE;

@Component
public class PayoutDaoImpl extends AbstractDao implements PayoutDao {

    @Autowired
    public PayoutDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long savePayout(Payout payout) {
        return getDslContext()
                .insertInto(PAYOUT)
                .set(getDslContext().newRecord(PAYOUT, payout))
                .onConflict(PAYOUT.PAYOUT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT, payout))
                .returning(PAYOUT.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public Payout getPayout(String payoutId) {
        PayoutRecord payoutRecord = getDslContext()
                .selectFrom(PAYOUT)
                .where(PAYOUT.PAYOUT_ID.eq(payoutId))
                .fetchOne();
        return payoutRecord == null ? null : payoutRecord.into(Payout.class);
    }

    @Override
    public void savePayoutAccountInfo(PayoutAccount payoutAccount) {
        getDslContext()
                .insertInto(PAYOUT_ACCOUNT)
                .set(getDslContext().newRecord(PAYOUT_ACCOUNT, payoutAccount))
                .onConflict(PAYOUT_ACCOUNT.EXT_PAYOUT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT_ACCOUNT, payoutAccount))
                .execute();
    }

    @Override
    public PayoutAccount getPayoutAccount(Long extPayoutId) {
        PayoutAccountRecord payoutAccountRecord = getDslContext()
                .selectFrom(PAYOUT_ACCOUNT)
                .where(PAYOUT_ACCOUNT.EXT_PAYOUT_ID.eq(extPayoutId))
                .fetchOne();
        return payoutAccountRecord == null ? null : payoutAccountRecord.into(PayoutAccount.class);
    }

    @Override
    public void savePayoutInternationalAccountInfo(PayoutInternationalAccount internationalAccount) {
        getDslContext()
                .insertInto(PAYOUT_INTERNATIONAL_ACCOUNT)
                .set(getDslContext().newRecord(PAYOUT_INTERNATIONAL_ACCOUNT, internationalAccount))
                .onConflict(PAYOUT_INTERNATIONAL_ACCOUNT.EXT_PAYOUT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT_INTERNATIONAL_ACCOUNT, internationalAccount))
                .execute();
    }

    @Override
    public PayoutInternationalAccount getPayoutInternationalAccount(Long extPayoutId) {
        PayoutInternationalAccountRecord accountRecord = getDslContext()
                .selectFrom(PAYOUT_INTERNATIONAL_ACCOUNT)
                .where(PAYOUT_INTERNATIONAL_ACCOUNT.EXT_PAYOUT_ID.eq(extPayoutId))
                .fetchOne();
        return accountRecord == null ? null : accountRecord.into(PayoutInternationalAccount.class);
    }

    @Override
    public Long savePayoutState(PayoutState payoutState) {
        return getDslContext()
                .insertInto(PAYOUT_STATE)
                .set(getDslContext().newRecord(PAYOUT_STATE, payoutState))
                .onConflict(PAYOUT_STATE.EXT_PAYOUT_ID, PAYOUT_STATE.EVENT_CREATED_AT, PAYOUT_STATE.STATUS)
                .doUpdate()
                .set(getDslContext().newRecord(PAYOUT_STATE, payoutState))
                .returning(PAYOUT_STATE.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public PayoutState getPayoutState(Long extPayoutId) {
        PayoutStateRecord payoutStateRecord = getDslContext()
                .selectFrom(PAYOUT_STATE)
                .where(PAYOUT_STATE.EXT_PAYOUT_ID.eq(extPayoutId))
                .fetchOne();
        return payoutStateRecord == null ? null : payoutStateRecord.into(PayoutState.class);
    }

    @Override
    public Optional<Long> getLastEventId() {
        return Optional.ofNullable(
                getDslContext()
                        .select(DSL.max(PAYOUT_STATE.EVENT_ID))
                        .from(PAYOUT_STATE).fetchOne()
                        .value1()
        );

    }

    @Override
    public Optional<LocalDateTime> getLastAggregationDate() {
        return getLastPayoutAggsByHourDateTime().or(() -> getFirstPayoutDateTime());
    }

    private Optional<LocalDateTime> getLastPayoutAggsByHourDateTime() {
        return Optional.ofNullable(getDslContext()
                .selectFrom(PAYOUT_AGGS_BY_HOUR)
                .orderBy(PAYOUT_AGGS_BY_HOUR.CREATED_AT.desc())
                .limit(1)
                .fetchOne())
                .map(PayoutAggsByHourRecord::getCreatedAt);
    }

    private Optional<LocalDateTime> getFirstPayoutDateTime() {
        return Optional.ofNullable(getDslContext()
                .selectFrom(PAYOUT)
                .orderBy(PAYOUT.CREATED_AT.asc())
                .limit(1)
                .fetchOne())
                .map(PayoutRecord::getCreatedAt);
    }

    @Override
    public void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sql = "INSERT INTO rpt.payout_aggs_by_hour (created_at, party_id, shop_id, amount, fee, currency_code, type)" +
                "SELECT date_trunc('hour', ps.event_created_at), pay.party_id, \n" +
                "       pay.shop_id, sum(pay.amount), sum(pay.fee), pay.currency_code, pay.type \n" +
                "FROM rpt.payout_state as ps \n" +
                "JOIN rpt.payout as pay on pay.id = ps.ext_payout_id \n" +
                "WHERE ps.event_created_at >= {0} AND ps.event_created_at < {1} \n" +
                "  AND ps.status = {2}" +
                "GROUP BY date_trunc('hour', ps.event_created_at), pay.party_id, " +
                "         pay.shop_id, pay.currency_code, pay.type \n" +
                "ON CONFLICT (party_id, shop_id, created_at, type, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount, fee = EXCLUDED.fee;";
        getDslContext().execute(sql, dateFrom, dateTo, PayoutStatus.paid);
    }

    @Override
    public List<PayoutAggsByHourRecord> getPayoutsAggsByHour(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return getDslContext()
                .selectFrom(PAYOUT_AGGS_BY_HOUR)
                .where(PAYOUT_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(PAYOUT_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }

}
