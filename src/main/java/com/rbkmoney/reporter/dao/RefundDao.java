package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.RefundAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import org.jooq.Cursor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundDao {

    Long saveRefund(Refund refund);

    List<Refund> getRefundsByState(LocalDateTime dateFrom,
                                   LocalDateTime dateTo,
                                   List<RefundStatus> statuses);

    void saveAdditionalRefundInfo(RefundAdditionalInfo refundAdditionalInfo);

    Cursor<RefundRecord> getRefundsCursor(String partyId,
                                          String shopId,
                                          LocalDateTime fromTime,
                                          LocalDateTime toTime);

    Long getFundsRefundedAmount(String partyId,
                                String partyShopId,
                                String currencyCode,
                                Optional<LocalDateTime> fromTime,
                                LocalDateTime toTime);

}
