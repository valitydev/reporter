package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.AdjustmentDao;
import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.reporter.domain.tables.Adjustment.ADJUSTMENT;
import static com.rbkmoney.reporter.domain.tables.AdjustmentAggsByHour.ADJUSTMENT_AGGS_BY_HOUR;

@Component
public class AdjustmentDaoImpl extends AbstractDao implements AdjustmentDao {

    @Autowired
    public AdjustmentDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long saveAdjustment(Adjustment adjustment) {
        return getDslContext()
                .insertInto(ADJUSTMENT)
                .set(getDslContext().newRecord(ADJUSTMENT, adjustment))
                .onConflict(ADJUSTMENT.INVOICE_ID, ADJUSTMENT.PAYMENT_ID, ADJUSTMENT.ADJUSTMENT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(ADJUSTMENT, adjustment))
                .returning(ADJUSTMENT.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public List<Adjustment> getAdjustmentsByState(LocalDateTime dateFrom,
                                                  LocalDateTime dateTo,
                                                  List<AdjustmentStatus> statuses) {
        Result<AdjustmentRecord> records = getDslContext()
                .selectFrom(ADJUSTMENT)
                .where(ADJUSTMENT.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(ADJUSTMENT.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(ADJUSTMENT.STATUS.in(statuses)))
                .fetch();
        return records == null || records.isEmpty() ? new ArrayList<>() : records.into(Adjustment.class);
    }

    @Override
    public Cursor<AdjustmentRecord> getAdjustmentCursor(String partyId,
                                                        String shopId,
                                                        LocalDateTime fromTime,
                                                        LocalDateTime toTime) {
        return getDslContext()
                .selectFrom(ADJUSTMENT)
                .where(ADJUSTMENT.STATUS_CREATED_AT.greaterThan(fromTime))
                .and(ADJUSTMENT.STATUS_CREATED_AT.lessThan(toTime))
                .and(ADJUSTMENT.PARTY_ID.eq(partyId))
                .and(ADJUSTMENT.SHOP_ID.eq(shopId))
                .and(ADJUSTMENT.STATUS.eq(AdjustmentStatus.captured))
                .fetchLazy();
    }

    @Override
    public Optional<LocalDateTime> getLastAggregationDate() {
        return getLastAdjustmentAggsByHourLocalDateTime()
                .or(this::getFirstAdjustmentLocalDateTime);
    }

    @Override
    public void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sql = "INSERT INTO rpt.adjustment_aggs_by_hour (created_at, party_id, shop_id, amount, currency_code)" +
                "SELECT date_trunc('hour', status_created_at), party_id, shop_id, sum(amount), currency_code \n" +
                "FROM  rpt.adjustment \n" +
                "WHERE status_created_at >= {0} AND status_created_at < {1} \n" +
                "  AND status = {2} \n" +
                "GROUP BY date_trunc('hour', status_created_at), party_id, shop_id, currency_code \n" +
                "ON CONFLICT (party_id, shop_id, created_at, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount;";
        getDslContext().execute(sql, dateFrom, dateTo, AdjustmentStatus.captured);
    }

    @Override
    public List<AdjustmentAggsByHourRecord> getAdjustmentsAggsByHour(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return getDslContext()
                .selectFrom(ADJUSTMENT_AGGS_BY_HOUR)
                .where(ADJUSTMENT_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(ADJUSTMENT_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }

    private Optional<LocalDateTime> getLastAdjustmentAggsByHourLocalDateTime() {
        return Optional.ofNullable(
                getDslContext()
                        .selectFrom(ADJUSTMENT_AGGS_BY_HOUR)
                        .orderBy(ADJUSTMENT_AGGS_BY_HOUR.CREATED_AT.desc())
                        .limit(1)
                        .fetchOne())
                .map(AdjustmentAggsByHourRecord::getCreatedAt);
    }

    private Optional<LocalDateTime> getFirstAdjustmentLocalDateTime() {
        return Optional.ofNullable(
                getDslContext()
                        .selectFrom(ADJUSTMENT)
                        .orderBy(ADJUSTMENT.CREATED_AT.asc())
                        .limit(1)
                        .fetchOne())
                .map(AdjustmentRecord::getCreatedAt);
    }
}
