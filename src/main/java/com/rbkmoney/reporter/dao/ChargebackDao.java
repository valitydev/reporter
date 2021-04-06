package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.tables.pojos.Chargeback;
import com.rbkmoney.reporter.domain.tables.records.ChargebackRecord;
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
