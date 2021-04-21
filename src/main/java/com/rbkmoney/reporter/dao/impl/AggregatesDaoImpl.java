package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.AggregatesDao;
import com.rbkmoney.reporter.domain.enums.*;
import com.rbkmoney.reporter.domain.tables.pojos.LastAggregationTime;
import com.rbkmoney.reporter.domain.tables.records.*;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.reporter.domain.Tables.PAYOUT;
import static com.rbkmoney.reporter.domain.Tables.REFUND;
import static com.rbkmoney.reporter.domain.tables.Adjustment.ADJUSTMENT;
import static com.rbkmoney.reporter.domain.tables.AdjustmentAggsByHour.ADJUSTMENT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.LastAggregationTime.LAST_AGGREGATION_TIME;
import static com.rbkmoney.reporter.domain.tables.Payment.PAYMENT;
import static com.rbkmoney.reporter.domain.tables.PaymentAggsByHour.PAYMENT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.PayoutAggsByHour.PAYOUT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.RefundAggsByHour.REFUND_AGGS_BY_HOUR;

@Component
public class AggregatesDaoImpl extends AbstractDao implements AggregatesDao {

    public AggregatesDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void saveLastAggregationDate(LastAggregationTime lastAggregationTime) {
        getDslContext()
                .insertInto(LAST_AGGREGATION_TIME)
                .set(getDslContext().newRecord(LAST_AGGREGATION_TIME, lastAggregationTime))
                .onConflict(LAST_AGGREGATION_TIME.AGGREGATION_TYPE, LAST_AGGREGATION_TIME.AGGREGATION_INTERVAL)
                .doUpdate()
                .set(getDslContext().newRecord(LAST_AGGREGATION_TIME, lastAggregationTime))
                .execute();
    }

    @Override
    public Optional<LocalDateTime> getLastAggregationDate(AggregationType aggregationType) {
        return getLastPaymentAggregationByHourDate(aggregationType)
                .or(() -> getFirstOperationDateTime(aggregationType));
    }

    @Override
    public List<PaymentAggsByHourRecord> getPaymentsAggregatesByHour(LocalDateTime dateFrom,
                                                                     LocalDateTime dateTo) {
        return getDslContext()
                .selectFrom(PAYMENT_AGGS_BY_HOUR)
                .where(PAYMENT_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(PAYMENT_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }

    @Override
    public void aggregateByHour(AggregationType aggregationType,
                                LocalDateTime dateFrom,
                                LocalDateTime dateTo) {
        switch (aggregationType) {
            case PAYMENT:
                aggregatePaymentsByHour(dateFrom, dateTo);
                break;
            case REFUND:
                aggregateRefundsByHour(dateFrom, dateTo);
                break;
            case ADJUSTMENT:
                aggregateAdjustmentsByHour(dateFrom, dateTo);
                break;
            case PAYOUT:
                aggregatePayoutsByHour(dateFrom, dateTo);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void aggregatePaymentsByHour(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sql = "INSERT INTO rpt.payment_aggs_by_hour (created_at, party_id, shop_id, amount, " +
                "                   origin_amount, currency_code, fee, provider_fee, external_fee)" +
                "SELECT date_trunc('hour', status_created_at) as created_at, \n" +
                "       party_id, shop_id, sum(amount) as amount, sum(origin_amount) as origin_amount, \n" +
                "       currency_code, sum(fee) as fee, sum(provider_fee) as provider_fee, \n" +
                "       sum(external_fee) as external_fee \n" +
                "FROM rpt.payment \n" +
                "WHERE status_created_at >= {0} AND status_created_at < {1} \n" +
                "  AND status = {2} \n" +
                "  AND party_id IS NOT NULL \n" +
                "  AND shop_id IS NOT NULL \n" +
                "GROUP BY date_trunc('hour', status_created_at), \n" +
                "         party_id, shop_id, currency_code \n" +
                "ON CONFLICT (party_id, shop_id, created_at, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount, origin_amount = EXCLUDED.origin_amount, fee = EXCLUDED.fee, " +
                "    provider_fee = EXCLUDED.provider_fee, external_fee = EXCLUDED.external_fee;";
        getDslContext().execute(sql, dateFrom, dateTo, InvoicePaymentStatus.captured);
    }

    @Override
    public List<RefundAggsByHourRecord> getRefundsAggregatesByHour(LocalDateTime dateFrom,
                                                                   LocalDateTime dateTo) {
        return getDslContext()
                .selectFrom(REFUND_AGGS_BY_HOUR)
                .where(REFUND_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(REFUND_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }

    @Override
    public void aggregateRefundsByHour(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sql = "INSERT INTO rpt.refund_aggs_by_hour (created_at, party_id, shop_id, amount, currency_code, " +
                "     fee, provider_fee, external_fee)" +
                "SELECT date_trunc('hour', status_created_at), party_id, shop_id, \n" +
                "       sum(amount), currency_code, sum(fee), sum(provider_fee), sum(external_fee) \n" +
                "FROM  rpt.refund \n" +
                "WHERE status_created_at >= {0} AND status_created_at < {1} \n" +
                "  AND status = {2} \n" +
                "  AND party_id IS NOT NULL \n" +
                "  AND shop_id IS NOT NULL \n" +
                "GROUP BY date_trunc('hour', status_created_at), party_id, shop_id, currency_code \n" +
                "ON CONFLICT (party_id, shop_id, created_at, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount, fee = EXCLUDED.fee, provider_fee = EXCLUDED.provider_fee, " +
                "    external_fee = EXCLUDED.external_fee;";
        getDslContext().execute(sql, dateFrom, dateTo, RefundStatus.succeeded);
    }

    @Override
    public List<PayoutAggsByHourRecord> getPayoutsAggregatesByHour(LocalDateTime dateFrom,
                                                                   LocalDateTime dateTo) {
        return getDslContext()
                .selectFrom(PAYOUT_AGGS_BY_HOUR)
                .where(PAYOUT_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(PAYOUT_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }

    @Override
    public void aggregatePayoutsByHour(LocalDateTime dateFrom,
                                       LocalDateTime dateTo) {
        String sql = "with paid_payouts as (\n" +
                "    SELECT date_trunc('hour', ps.event_created_at) as created_at, \n" +
                "           pay.party_id, pay.shop_id, (sum(pay.amount) - sum(pay.fee)) as amount, \n" +
                "           pay.currency_code, pay.type \n" +
                "    FROM rpt.payout_state as ps \n" +
                "    JOIN rpt.payout as pay on pay.payout_id = ps.payout_id  \n" +
                "    WHERE ps.event_created_at >= {0} AND ps.event_created_at < {1} \n" +
                "      AND ps.status = {2} \n" +
                "      AND party_id IS NOT NULL \n" +
                "      AND shop_id IS NOT NULL \n" +
                "    GROUP BY date_trunc('hour', ps.event_created_at), \n" +
                "             pay.party_id, pay.shop_id, pay.currency_code, pay.type \n" +
                "), \n" +
                "cancelled_payouts as (\n" +
                "    SELECT date_trunc('hour', ps.event_created_at) as created_at, \n" +
                "           pay.party_id, pay.shop_id, -1 * (sum(pay.amount) - sum(pay.fee)) as amount, \n" +
                "           pay.currency_code, pay.type \n" +
                "    FROM rpt.payout_state as ps \n" +
                "    JOIN rpt.payout as pay on pay.payout_id = ps.payout_id  \n" +
                "    JOIN rpt.payout_state as ps_success " +
                "      ON ps.payout_id = ps_success.payout_id and ps_success.status = {2} \n" +
                "    WHERE ps.event_created_at >= {0} AND ps.event_created_at < {1} \n" +
                "      AND ps.status = {3} \n" +
                "      AND party_id IS NOT NULL  \n" +
                "      AND shop_id IS NOT NULL  \n" +
                "    GROUP BY date_trunc('hour', ps.event_created_at), \n" +
                "             pay.party_id, pay.shop_id, pay.currency_code, pay.type \n" +
                ") \n" +
                "\n" +
                "INSERT INTO rpt.payout_aggs_by_hour " +
                "      (created_at, party_id, shop_id, amount, currency_code, type)" +
                "SELECT created_at, party_id, shop_id, sum(amount), currency_code, type \n" +
                "FROM (\n" +
                "    SELECT * FROM paid_payouts \n" +
                "        UNION ALL \n" +
                "    SELECT * FROM cancelled_payouts \n" +
                ") as total\n" +
                "GROUP BY created_at, party_id, shop_id, currency_code, type \n" +
                "ORDER BY 1 \n" +
                "ON CONFLICT (party_id, shop_id, created_at, type, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount; \n";
        getDslContext().execute(sql, dateFrom, dateTo, PayoutStatus.paid, PayoutStatus.cancelled);
    }

    @Override
    public List<AdjustmentAggsByHourRecord> getAdjustmentsAggregatesByHour(LocalDateTime dateFrom,
                                                                           LocalDateTime dateTo) {
        return getDslContext()
                .selectFrom(ADJUSTMENT_AGGS_BY_HOUR)
                .where(ADJUSTMENT_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(ADJUSTMENT_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }

    @Override
    public void aggregateAdjustmentsByHour(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sql = "INSERT INTO rpt.adjustment_aggs_by_hour (created_at, party_id, shop_id, amount, currency_code)" +
                "SELECT date_trunc('hour', status_created_at), party_id, shop_id, sum(amount), currency_code \n" +
                "FROM  rpt.adjustment \n" +
                "WHERE status_created_at >= {0} AND status_created_at < {1} \n" +
                "  AND status = {2} \n" +
                "  AND party_id IS NOT NULL \n" +
                "  AND shop_id IS NOT NULL \n" +
                "GROUP BY date_trunc('hour', status_created_at), party_id, shop_id, currency_code \n" +
                "ON CONFLICT (party_id, shop_id, created_at, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount;";
        getDslContext().execute(sql, dateFrom, dateTo, AdjustmentStatus.captured);
    }

    private Optional<LocalDateTime> getLastPaymentAggregationByHourDate(AggregationType aggregationType) {
        return Optional.ofNullable(
                getDslContext()
                        .selectFrom(LAST_AGGREGATION_TIME)
                        .where(LAST_AGGREGATION_TIME.AGGREGATION_TYPE.eq(aggregationType))
                        .and(LAST_AGGREGATION_TIME.AGGREGATION_INTERVAL.eq(AggregationInterval.HOUR))
                        .fetchOne())
                .map(LastAggregationTimeRecord::getLastDataAggregationDate);
    }

    private Optional<LocalDateTime> getFirstOperationDateTime(AggregationType aggregationType) {
        switch (aggregationType) {
            case PAYMENT:
                return getFirstPaymentDateTime();
            case REFUND:
                return getFirstRefundDateTime();
            case ADJUSTMENT:
                return getFirstAdjustmentDateTime();
            case PAYOUT:
                return getFirstPayoutDateTime();
            default:
                return Optional.empty();
        }
    }

    private Optional<LocalDateTime> getFirstPaymentDateTime() {
        return Optional.ofNullable(
                getDslContext()
                        .selectFrom(PAYMENT)
                        .orderBy(PAYMENT.CREATED_AT.asc())
                        .limit(1)
                        .fetchOne())
                .map(PaymentRecord::getCreatedAt);
    }

    private Optional<LocalDateTime> getFirstRefundDateTime() {
        return Optional.ofNullable(
                getDslContext()
                        .selectFrom(REFUND)
                        .orderBy(REFUND.CREATED_AT.asc())
                        .limit(1)
                        .fetchOne())
                .map(RefundRecord::getCreatedAt);
    }

    private Optional<LocalDateTime> getFirstAdjustmentDateTime() {
        return Optional.ofNullable(
                getDslContext()
                        .selectFrom(ADJUSTMENT)
                        .orderBy(ADJUSTMENT.CREATED_AT.asc())
                        .limit(1)
                        .fetchOne())
                .map(AdjustmentRecord::getCreatedAt);
    }

    private Optional<LocalDateTime> getFirstPayoutDateTime() {
        return Optional.ofNullable(
                getDslContext()
                        .selectFrom(PAYOUT)
                        .orderBy(PAYOUT.CREATED_AT.asc())
                        .limit(1)
                        .fetchOne())
                .map(PayoutRecord::getCreatedAt);
    }

}
