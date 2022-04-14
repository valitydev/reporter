package dev.vality.reporter.dao;

import dev.vality.reporter.domain.enums.AdjustmentStatus;
import dev.vality.reporter.domain.tables.pojos.Adjustment;
import dev.vality.reporter.domain.tables.records.AdjustmentRecord;
import org.jooq.Cursor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdjustmentDao {

    Long saveAdjustment(Adjustment adjustment);

    List<Adjustment> getAdjustmentsByState(LocalDateTime dateFrom,
                                           LocalDateTime dateTo,
                                           List<AdjustmentStatus> statuses);

    Cursor<AdjustmentRecord> getAdjustmentCursor(String partyId,
                                                 String shopId,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime);

    Long getFundsAdjustedAmount(String partyId,
                                String shopId,
                                String currencyCode,
                                Optional<LocalDateTime> fromTime,
                                LocalDateTime toTime);
}
