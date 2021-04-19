package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.ChargebackDao;
import com.rbkmoney.reporter.domain.enums.ChargebackStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.records.ChargebackRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.rbkmoney.reporter.domain.Tables.CHARGEBACK;
import static com.rbkmoney.reporter.util.AccountingDaoUtils.getFundsAmountResult;

@Component
public class ChargebackDaoImpl extends AbstractDao implements ChargebackDao {

    private static final String AMOUNT_KEY = "funds_returned";

    public ChargebackDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long saveChargeback(Chargeback chargeback) {
        return getDslContext()
                .insertInto(CHARGEBACK)
                .set(getDslContext().newRecord(CHARGEBACK, chargeback))
                .onConflict(CHARGEBACK.INVOICE_ID, CHARGEBACK.PAYMENT_ID, CHARGEBACK.CHARGEBACK_ID, CHARGEBACK.STAGE)
                .doUpdate()
                .set(getDslContext().newRecord(CHARGEBACK, chargeback))
                .returning(CHARGEBACK.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public Cursor<ChargebackRecord> getChargebackCursor(String partyId,
                                                        String shopId,
                                                        LocalDateTime fromTime,
                                                        LocalDateTime toTime) {
        return getDslContext()
                .selectFrom(CHARGEBACK)
                .where(CHARGEBACK.EVENT_CREATED_AT.greaterThan(fromTime))
                .and(CHARGEBACK.EVENT_CREATED_AT.lessThan(toTime))
                .and(CHARGEBACK.PARTY_ID.eq(partyId))
                .and(CHARGEBACK.SHOP_ID.eq(shopId))
                .and(CHARGEBACK.STATUS.eq(ChargebackStatus.accepted))
                .orderBy(CHARGEBACK.EVENT_CREATED_AT)
                .fetchLazy();
    }

    @Override
    public Long getFundsReturnedAmount(String partyId,
                                       String shopId,
                                       String currencyCode,
                                       Optional<LocalDateTime> fromTime,
                                       LocalDateTime toTime) {
        var fundsReturnedAmount = getDslContext()
                .select(DSL.sum(CHARGEBACK.AMOUNT).as(AMOUNT_KEY))
                .from(CHARGEBACK)
                .where(fromTime.map(CHARGEBACK.CREATED_AT::ge).orElse(DSL.trueCondition()))
                .and(CHARGEBACK.CREATED_AT.lessThan(toTime))
                .and(CHARGEBACK.CURRENCY_CODE.eq(currencyCode))
                .and(CHARGEBACK.PARTY_ID.eq(partyId))
                .and(CHARGEBACK.SHOP_ID.eq(shopId))
                .fetchOne();
        return getFundsAmountResult(fundsReturnedAmount);
    }
}
