package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import org.jooq.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AdjustmentDao {

    Long saveAdjustment(Adjustment adjustment);

    Query getSaveAdjustmentQuery(Adjustment adjustment);

    List<Adjustment> getAdjustmentsByState(LocalDateTime dateFrom,
                                           LocalDateTime dateTo,
                                           List<AdjustmentStatus> statuses);

}
