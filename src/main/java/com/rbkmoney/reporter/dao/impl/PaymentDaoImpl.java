package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.PaymentAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.PaymentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.rbkmoney.reporter.domain.tables.Payment.PAYMENT;
import static com.rbkmoney.reporter.domain.tables.PaymentAdditionalInfo.PAYMENT_ADDITIONAL_INFO;
import static com.rbkmoney.reporter.domain.tables.PaymentAggsByHour.PAYMENT_AGGS_BY_HOUR;

import org.jooq.impl.*;

@Component
public class PaymentDaoImpl extends AbstractDao implements PaymentDao {

    @Autowired
    public PaymentDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long savePayment(Payment payment) {
        return getDslContext()
                .insertInto(PAYMENT)
                .set(getDslContext().newRecord(PAYMENT, payment))
                .onConflict(PAYMENT.INVOICE_ID, PAYMENT.PAYMENT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYMENT, payment))
                .returning(PAYMENT.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public List<Payment> getPaymentsByState(LocalDateTime dateFrom,
                                            LocalDateTime dateTo,
                                            List<InvoicePaymentStatus> statuses) {
        Result<PaymentRecord> records = getDslContext()
                .selectFrom(PAYMENT)
                .where(PAYMENT.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(PAYMENT.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(PAYMENT.STATUS.in(statuses)))
                .fetch();
        return records == null || records.isEmpty() ? new ArrayList<>() : records.into(Payment.class);
    }

    @Override
    public void saveAdditionalPaymentInfo(PaymentAdditionalInfo paymentAdditionalInfo) {
        getDslContext()
                .insertInto(PAYMENT_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(PAYMENT_ADDITIONAL_INFO, paymentAdditionalInfo))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(PAYMENT_ADDITIONAL_INFO, paymentAdditionalInfo))
                .execute();
    }

    @Override
    public Map<String, Long> getShopAccountingReportData(String partyId,
                                                         String partyShopId,
                                                         String currencyCode,
                                                         Optional<LocalDateTime> fromTime,
                                                         LocalDateTime toTime) {
        String amountKey = "funds_acquired";
        String feeKey = "fee_charged";

        var conditionStep = getDslContext().select(
                DSL.sum(PAYMENT.AMOUNT).as(amountKey),
                DSL.sum(DSL.coalesce(PAYMENT.FEE, 0L)).as(feeKey)
        )
                .from(PAYMENT)
                .where(fromTime.map(PAYMENT.CREATED_AT::ge).orElse(DSL.trueCondition()))
                .and(PAYMENT.CREATED_AT.lt(toTime))
                .and(PAYMENT.STATUS.eq(InvoicePaymentStatus.captured))
                .and(PAYMENT.CURRENCY_CODE.eq(currencyCode))
                .and(PAYMENT.PARTY_ID.eq(partyId))
                .and(PAYMENT.SHOP_ID.eq(partyShopId));

        Record2<BigDecimal, BigDecimal> result = conditionStep.fetchOne();
        Map<String, Long> accountingData = new HashMap<>();
        if (result == null) {
            accountingData.put(amountKey, 0L);
            accountingData.put(feeKey, 0L);
        } else {
            accountingData.put(amountKey, result.value1().longValue());
            accountingData.put(feeKey, result.value2().longValue());
        }
        return accountingData;
    }

    @Override
    public PaymentRecord getPayment(String partyId, String shopId, String invoiceId, String paymentId) {
        return getDslContext()
                .selectFrom(PAYMENT)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId))
                .and(PAYMENT.PAYMENT_ID.eq(paymentId))
                .and(PAYMENT.PARTY_ID.eq(partyId))
                .and(PAYMENT.SHOP_ID.eq(shopId))
                .fetchOne();
    }

    @Override
    public Cursor<PaymentRecord> getPaymentsCursor(String partyId,
                                                   String shopId,
                                                   Optional<LocalDateTime> fromTime,
                                                   LocalDateTime toTime) {
        return getDslContext()
                .selectFrom(PAYMENT)
                .where(fromTime.map(PAYMENT.STATUS_CREATED_AT::ge).orElse(DSL.trueCondition()))
                .and(PAYMENT.STATUS_CREATED_AT.lt(toTime))
                .and(PAYMENT.PARTY_ID.eq(partyId))
                .and(PAYMENT.SHOP_ID.eq(shopId))
                .and(PAYMENT.STATUS.eq(InvoicePaymentStatus.captured))
                .fetchLazy();
    }

    @Override
    public Optional<LocalDateTime> getLastAggregationDate() {
        return getLastPaymentAggsByHourDateTime().or(() -> getFirstPaymentDateTime());
    }

    private Optional<LocalDateTime> getLastPaymentAggsByHourDateTime() {
        return Optional.ofNullable(getDslContext()
                .selectFrom(PAYMENT_AGGS_BY_HOUR)
                .orderBy(PAYMENT_AGGS_BY_HOUR.CREATED_AT.desc())
                .limit(1)
                .fetchOne())
                .map(PaymentAggsByHourRecord::getCreatedAt);
    }

    private Optional<LocalDateTime> getFirstPaymentDateTime() {
        return Optional.ofNullable(getDslContext()
                .selectFrom(PAYMENT)
                .orderBy(PAYMENT.CREATED_AT.asc())
                .limit(1)
                .fetchOne())
                .map(PaymentRecord::getCreatedAt);
    }

    @Override
    public void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sql = "INSERT INTO rpt.payment_aggs_by_hour (created_at, party_id, shop_id, amount, " +
                "                   origin_amount, currency_code, fee, provider_fee, external_fee)" +
                "SELECT date_trunc('hour', status_created_at) as created_at, \n" +
                "       party_id, shop_id, sum(amount) as amount, sum(origin_amount) as origin_amount, \n" +
                "       currency_code, sum(fee) as fee, sum(provider_fee) as provider_fee, \n" +
                "       sum(external_fee) as external_fee \n" +
                "FROM rpt.payment \n" +
                "WHERE status_created_at >= {0} AND status_created_at < {1} \n" +
                "  AND status = {2} \n" +
                "GROUP BY date_trunc('hour', status_created_at), \n" +
                "         party_id, shop_id, currency_code \n" +
                "ON CONFLICT (party_id, shop_id, created_at, currency_code) \n" +
                "DO UPDATE \n" +
                "SET amount = EXCLUDED.amount, origin_amount = EXCLUDED.origin_amount, fee = EXCLUDED.fee, " +
                "    provider_fee = EXCLUDED.provider_fee, external_fee = EXCLUDED.external_fee;";
        getDslContext().execute(sql, dateFrom, dateTo, InvoicePaymentStatus.captured);
    }

    @Override
    public List<PaymentAggsByHourRecord> getPaymentsAggregatesByHour(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return getDslContext()
                .selectFrom(PAYMENT_AGGS_BY_HOUR)
                .where(PAYMENT_AGGS_BY_HOUR.CREATED_AT.greaterOrEqual(dateFrom)
                        .and(PAYMENT_AGGS_BY_HOUR.CREATED_AT.lessThan(dateTo)))
                .fetch();
    }

}
