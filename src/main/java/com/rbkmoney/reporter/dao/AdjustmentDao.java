package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import org.jooq.Cursor;

import java.time.LocalDateTime;
import java.util.List;

public interface AdjustmentDao extends AggregatesDao {

    Long saveAdjustment(Adjustment adjustment);

    List<Adjustment> getAdjustmentsByState(LocalDateTime dateFrom,
                                           LocalDateTime dateTo,
                                           List<AdjustmentStatus> statuses);

    Cursor<AdjustmentRecord> getAdjustmentCursor(String partyId,
                                                 String shopId,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime);

    LocalDateTime getLastAggregationDate();

    void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo);

    List<AdjustmentAggsByHourRecord> getAdjustmentsAggsByHour(LocalDateTime dateFrom, LocalDateTime dateTo);
}
