package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.RefundDao;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.RefundAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.RefundAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.Refund.REFUND;
import static com.rbkmoney.reporter.domain.tables.RefundAdditionalInfo.REFUND_ADDITIONAL_INFO;
import static com.rbkmoney.reporter.domain.tables.RefundAggsByHour.REFUND_AGGS_BY_HOUR;

@Component
public class RefundDaoImpl extends AbstractDao implements RefundDao {

    @Autowired
    public RefundDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long saveRefund(Refund refund) {
        return getDslContext()
                .insertInto(REFUND)
                .set(getDslContext().newRecord(REFUND, refund))
                .onConflict(REFUND.INVOICE_ID, REFUND.PAYMENT_ID, REFUND.REFUND_ID)
                .doUpdate()
                .set(getDslContext().newRecord(REFUND, refund))
                .returning(REFUND.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public List<Refund> getRefundsByState(LocalDateTime dateFrom,
                                          LocalDateTime dateTo,
                                          List<RefundStatus> statuses) {
        Result<RefundRecord> records = getDslContext()
                .selectFrom(REFUND)
                .where(REFUND.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(REFUND.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(REFUND.STATUS.in(statuses)))
                .fetch();
        return records == null || records.isEmpty() ? new ArrayList<>() : records.into(Refund.class);
    }

    @Override
    public void saveAdditionalRefundInfo(RefundAdditionalInfo refundAdditionalInfo) {
        getDslContext()
                .insertInto(REFUND_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(REFUND_ADDITIONAL_INFO, refundAdditionalInfo))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(REFUND_ADDITIONAL_INFO, refundAdditionalInfo))
        .execute();

    }

    @Override
    public Cursor<RefundRecord> getRefundsCursor(String partyId,
                                                 String shopId,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime) {
        return getDslContext()
                .selectFrom(REFUND)
                .where(REFUND.STATUS_CREATED_AT.greaterThan(fromTime))
                .and(REFUND.STATUS_CREATED_AT.lessThan(toTime))
                .and(REFUND.PARTY_ID.eq(partyId))
                .and(REFUND.SHOP_ID.eq(shopId))
                .and(REFUND.STATUS.eq(RefundStatus.succeeded))
                .fetchLazy();
    }

    @Override
    public LocalDateTime getLastAggregationDate() {
        return getDslContext()
                .selectFrom(REFUND_AGGS_BY_HOUR)
                .orderBy(REFUND_AGGS_BY_HOUR.CREATED_AT.desc())
                .limit(1)
                .fetchOne()
                .getCreatedAt();
    }

    @Override
    public void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sql = "INSERT INTO rpt.refund_aggs_by_hour (created_at, party_id, shop_id, amount, currency_code, " +
                "     fee, provider_fee, external_fee)" +
                "SELECT date_trunc('hour', status_created_at), party_id, shop_id, \n" +
                "       sum(amount), currency_code, sum(fee), sum(provider_fee), sum(external_fee) \n" +
                "FROM  rpt.refund \n" +
                "WHERE status_created_at >= {0} AND status_created_at < {1} \n" +
                "  AND status = {2} \n" +
                "GROUP BY date_trunc('hour', status_created_at), party_id, shop_id, currency_code \n" +
                "ON CONFLICT (party_id, shop_id, created_at, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount, fee = EXCLUDED.fee, provider_fee = EXCLUDED.provider_fee, " +
                "    external_fee = EXCLUDED.external_fee;";
        getDslContext().execute(sql, dateFrom, dateTo, RefundStatus.succeeded);
    }

    @Override
    public List<RefundAggsByHourRecord> getRefundAggsByHour(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return  getDslContext()
                .selectFrom(REFUND_AGGS_BY_HOUR)
                .where(REFUND_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(REFUND_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }
}
