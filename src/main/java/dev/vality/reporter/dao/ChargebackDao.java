package dev.vality.reporter.dao;

import dev.vality.reporter.domain.tables.pojos.Chargeback;
import dev.vality.reporter.domain.tables.records.ChargebackRecord;
import org.jooq.Cursor;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChargebackDao {

    Long saveChargeback(Chargeback chargeback);

    Cursor<ChargebackRecord> getChargebackCursor(String partyId,
                                                 String shopId,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime);

    Long getFundsReturnedAmount(String partyId,
                                String shopId,
                                String currencyCode,
                                Optional<LocalDateTime> fromTime,
                                LocalDateTime toTime);

}
