package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.RefundDao;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.RefundAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.reporter.domain.Tables.REFUND_AGGS_BY_HOUR;
import static com.rbkmoney.reporter.domain.tables.Refund.REFUND;
import static com.rbkmoney.reporter.domain.tables.RefundAdditionalInfo.REFUND_ADDITIONAL_INFO;
import static com.rbkmoney.reporter.util.AccountingDaoUtils.getFundsAmountResult;

@Component
public class RefundDaoImpl extends AbstractDao implements RefundDao {

    private static final String AMOUNT_KEY = "funds_refunded";

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
    public Long getFundsRefundedAmount(String partyId,
                                       String partyShopId,
                                       String currencyCode,
                                       Optional<LocalDateTime> fromTime,
                                       LocalDateTime toTime) {
        LocalDateTime reportFromTime = fromTime.orElse(LocalDateTime.now());
        LocalDateTime fromTimeTruncHour = reportFromTime.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime toTimeTruncHour = toTime.truncatedTo(ChronoUnit.HOURS);

        var youngRefundShopAccountingQuery = getRefundFundsAmountQuery(
                partyId, partyShopId, currencyCode, reportFromTime, fromTimeTruncHour.plusHours(1L)
        );
        var refundAggByHourShopAccountingQuery = getAggByHourRefundFundsAmountQuery(
                partyId, partyShopId, currencyCode, fromTimeTruncHour, toTimeTruncHour
        );
        var oldRefundShopAccountingQuery = getRefundFundsAmountQuery(
                partyId, partyShopId, currencyCode, toTimeTruncHour, toTime
        );
        var fundsRefundedAmountResult = getDslContext()
                .select(DSL.sum(DSL.field(AMOUNT_KEY, Long.class)).as(AMOUNT_KEY))
                .from(
                        youngRefundShopAccountingQuery
                                .unionAll(refundAggByHourShopAccountingQuery)
                                .unionAll(oldRefundShopAccountingQuery)
                )
                .fetchOne();
        return getFundsAmountResult(fundsRefundedAmountResult);
    }

    private SelectConditionStep<Record1<BigDecimal>> getRefundFundsAmountQuery(String partyId,
                                                                               String partyShopId,
                                                                               String currencyCode,
                                                                               LocalDateTime fromTime,
                                                                               LocalDateTime toTime) {
        return getDslContext()
                .select(DSL.sum(REFUND.AMOUNT).as(AMOUNT_KEY))
                .from(REFUND)
                .where(REFUND.STATUS_CREATED_AT.greaterOrEqual(fromTime))
                .and(REFUND.STATUS_CREATED_AT.lessThan(toTime))
                .and(REFUND.STATUS.eq(RefundStatus.succeeded))
                .and(REFUND.CURRENCY_CODE.eq(currencyCode))
                .and(REFUND.PARTY_ID.eq(partyId))
                .and(REFUND.SHOP_ID.eq(partyShopId));
    }

    private SelectConditionStep<Record1<BigDecimal>> getAggByHourRefundFundsAmountQuery(String partyId,
                                                                                        String partyShopId,
                                                                                        String currencyCode,
                                                                                        LocalDateTime fromTime,
                                                                                        LocalDateTime toTime) {
        return getDslContext()
                .select(DSL.sum(REFUND_AGGS_BY_HOUR.AMOUNT).as(AMOUNT_KEY))
                .from(REFUND_AGGS_BY_HOUR)
                .where(REFUND_AGGS_BY_HOUR.CREATED_AT.greaterThan(fromTime))
                .and(REFUND_AGGS_BY_HOUR.CREATED_AT.lessThan(toTime))
                .and(REFUND_AGGS_BY_HOUR.CURRENCY_CODE.eq(currencyCode))
                .and(REFUND_AGGS_BY_HOUR.PARTY_ID.eq(partyId))
                .and(REFUND_AGGS_BY_HOUR.SHOP_ID.eq(partyShopId));
    }

}
