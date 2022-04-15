package dev.vality.reporter.dao.impl;

import dev.vality.reporter.dao.AbstractDao;
import dev.vality.reporter.dao.ChargebackDao;
import dev.vality.reporter.domain.enums.ChargebackStatus;
import dev.vality.reporter.domain.tables.pojos.Chargeback;
import dev.vality.reporter.domain.tables.records.ChargebackRecord;
import com.zaxxer.hikari.HikariDataSource;
import dev.vality.reporter.util.AccountingDaoUtils;
import org.jooq.Cursor;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static dev.vality.reporter.domain.Tables.CHARGEBACK;
import static java.util.Optional.ofNullable;
import static org.jooq.impl.DSL.trueCondition;

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
                .and(ofNullable(shopId).map(CHARGEBACK.SHOP_ID::eq).orElse(trueCondition()))
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
                .and(ofNullable(shopId).map(CHARGEBACK.SHOP_ID::eq).orElse(trueCondition()))
                .fetchOne();
        return AccountingDaoUtils.getFundsAmountResult(fundsReturnedAmount);
    }
}
