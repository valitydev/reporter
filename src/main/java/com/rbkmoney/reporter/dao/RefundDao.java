package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.RefundAdditionalInfo;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundDao {

    Long saveRefund(Refund refund);

    List<Refund> getRefundsByState(LocalDateTime dateFrom,
                                   LocalDateTime dateTo,
                                   List<RefundStatus> statuses);

    void saveAdditionalRefundInfo(RefundAdditionalInfo refundAdditionalInfo);

}
