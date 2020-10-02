package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.mapper.RecordRowMapper;
import com.rbkmoney.reporter.dao.AdjustmentDao;
import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.Adjustment.ADJUSTMENT;

@Component
public class AdjustmentDaoImpl extends AbstractGenericDao implements AdjustmentDao {

    private final RowMapper<Adjustment> adjustmentRowMapper;

    @Autowired
    public AdjustmentDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        adjustmentRowMapper = new RecordRowMapper<>(ADJUSTMENT, Adjustment.class);
    }

    @Override
    public Long saveAdjustment(Adjustment adjustment) {
        return fetchOne(getSaveAdjustmentQuery(adjustment), Long.class);
    }

    private Query getSaveAdjustmentQuery(Adjustment adjustment) {
        return getDslContext()
                .insertInto(ADJUSTMENT)
                .set(getDslContext().newRecord(ADJUSTMENT, adjustment))
                .onConflict(ADJUSTMENT.INVOICE_ID, ADJUSTMENT.PAYMENT_ID, ADJUSTMENT.ADJUSTMENT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(ADJUSTMENT, adjustment))
                .returning(ADJUSTMENT.ID);
    }

    @Override
    public List<Adjustment> getAdjustmentsByState(LocalDateTime dateFrom,
                                                  LocalDateTime dateTo,
                                                  List<AdjustmentStatus> statuses) {
        Query query = getDslContext()
                .selectFrom(ADJUSTMENT)
                .where(ADJUSTMENT.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(ADJUSTMENT.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(ADJUSTMENT.STATUS.in(statuses)));
        return fetch(query, adjustmentRowMapper);
    }
}
