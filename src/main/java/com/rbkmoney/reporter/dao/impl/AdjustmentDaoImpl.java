package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.AdjustmentDao;
import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.Adjustment.ADJUSTMENT;

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
        return getDslContext()
                .selectFrom(ADJUSTMENT)
                .where(ADJUSTMENT.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(ADJUSTMENT.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(ADJUSTMENT.STATUS.in(statuses)))
                .fetch()
                .into(Adjustment.class);
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
}
