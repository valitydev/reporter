package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.AggregationType;
import com.rbkmoney.reporter.domain.tables.pojos.LastAggregationTime;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.PayoutAggsByHourRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundAggsByHourRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AggregatesDao {

    void saveLastAggregationDate(LastAggregationTime lastAggregationTime);

    Optional<LocalDateTime> getLastAggregationDate(AggregationType aggregationType);

    List<PaymentAggsByHourRecord> getPaymentsAggregatesByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

    void aggregateByHour(AggregationType aggregationType, LocalDateTime dateFrom, LocalDateTime dateTo);

    void aggregatePaymentsByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

    List<RefundAggsByHourRecord> getRefundsAggregatesByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

    void aggregateRefundsByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

    List<PayoutAggsByHourRecord> getPayoutsAggregatesByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

    void aggregatePayoutsByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

    List<AdjustmentAggsByHourRecord> getAdjustmentsAggregatesByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

    void aggregateAdjustmentsByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

}
