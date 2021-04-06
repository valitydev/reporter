package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.PaymentAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.model.PaymentFundsAmount;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.rbkmoney.reporter.domain.tables.Payment.PAYMENT;
import static com.rbkmoney.reporter.domain.tables.PaymentAdditionalInfo.PAYMENT_ADDITIONAL_INFO;
import static com.rbkmoney.reporter.domain.tables.PaymentAggsByHour.PAYMENT_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.util.AccountingDaoUtils.getFunds;

@Component
public class PaymentDaoImpl extends AbstractDao implements PaymentDao {

    private static final String AMOUNT_KEY = "funds_acquired";
    private static final String FEE_KEY = "fee_charged";

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
        return records == null || records.isEmpty()
                ? new ArrayList<>() : records.into(Payment.class);
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
    public PaymentFundsAmount getPaymentFundsAmount(String partyId,
                                                    String shopId,
                                                    String currencyCode,
                                                    Optional<LocalDateTime> fromTime,
                                                    LocalDateTime toTime) {
        LocalDateTime reportFromTime = fromTime.orElse(LocalDateTime.now());
        LocalDateTime fromTimeTruncHour = reportFromTime.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime toTimeTruncHour = toTime.truncatedTo(ChronoUnit.HOURS);

        SelectConditionStep<Record2<BigDecimal, BigDecimal>> youngPaymentShopAccountingQuery =
                getPaymentFundsAmountQuery(
                    partyId, shopId, currencyCode, reportFromTime, fromTimeTruncHour.plusHours(1L)
            );

        var paymentAggByHourShopAccountingQuery = getAggByHourPaymentFundsAmountQuery(
                partyId, shopId, currencyCode, fromTimeTruncHour, toTimeTruncHour
        );
        var oldPaymentShopAccountingQuery = getPaymentFundsAmountQuery(
                partyId, shopId, currencyCode, toTimeTruncHour, toTime
        );

        var fundsResult = getDslContext().select(
                DSL.sum(DSL.field(AMOUNT_KEY, Long.class)).as(AMOUNT_KEY),
                DSL.sum(DSL.coalesce(DSL.field(FEE_KEY, Long.class), 0L)).as(FEE_KEY)
        ).from(
                youngPaymentShopAccountingQuery
                        .unionAll(paymentAggByHourShopAccountingQuery)
                        .unionAll(oldPaymentShopAccountingQuery)
        ).fetchOne();

        return Optional.ofNullable(fundsResult)
                .map(result -> new PaymentFundsAmount(getFunds(result.value1()), getFunds(result.value2())))
                .orElse(new PaymentFundsAmount(0L, 0L));
    }

    private SelectConditionStep<Record2<BigDecimal, BigDecimal>> getPaymentFundsAmountQuery(String partyId,
                                                                                            String partyShopId,
                                                                                            String currencyCode,
                                                                                            LocalDateTime fromTime,
                                                                                            LocalDateTime toTime) {
        return getDslContext()
                .select(
                        DSL.sum(PAYMENT.AMOUNT).as(AMOUNT_KEY),
                        DSL.sum(DSL.coalesce(PAYMENT.FEE, 0L)).as(FEE_KEY)
                )
                .from(PAYMENT)
                .where(PAYMENT.STATUS_CREATED_AT.greaterOrEqual(fromTime))
                .and(PAYMENT.STATUS_CREATED_AT.lessThan(toTime))
                .and(PAYMENT.STATUS.eq(InvoicePaymentStatus.captured))
                .and(PAYMENT.CURRENCY_CODE.eq(currencyCode))
                .and(PAYMENT.PARTY_ID.eq(partyId))
                .and(PAYMENT.SHOP_ID.eq(partyShopId));
    }

    private SelectConditionStep<Record2<BigDecimal, BigDecimal>> getAggByHourPaymentFundsAmountQuery(
            String partyId,
            String partyShopId,
            String currencyCode,
            LocalDateTime fromTime,
            LocalDateTime toTime
    ) {
        return getDslContext()
                .select(
                        DSL.sum(PAYMENT_AGGS_BY_HOUR.AMOUNT).as(AMOUNT_KEY),
                        DSL.sum(DSL.coalesce(PAYMENT_AGGS_BY_HOUR.FEE, 0L)).as(FEE_KEY)
                )
                .from(PAYMENT_AGGS_BY_HOUR)
                .where(PAYMENT_AGGS_BY_HOUR.CREATED_AT.greaterThan(fromTime))
                .and(PAYMENT_AGGS_BY_HOUR.CREATED_AT.lessThan(toTime))
                .and(PAYMENT_AGGS_BY_HOUR.CURRENCY_CODE.eq(currencyCode))
                .and(PAYMENT_AGGS_BY_HOUR.PARTY_ID.eq(partyId))
                .and(PAYMENT_AGGS_BY_HOUR.SHOP_ID.eq(partyShopId));
    }

    @Override
    public PaymentRecord getPayment(String partyId,
                                    String shopId,
                                    String invoiceId,
                                    String paymentId) {
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

}
