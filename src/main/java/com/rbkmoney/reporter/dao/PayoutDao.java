package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutInternationalAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;
import com.rbkmoney.reporter.domain.tables.records.PayoutAggsByHourRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutDao extends AggregatesDao {

    Long savePayout(Payout payout);

    Payout getPayout(String payoutId);

    void savePayoutAccountInfo(PayoutAccount payoutAccount);

    PayoutAccount getPayoutAccount(Long extPayoutId);

    void savePayoutInternationalAccountInfo(PayoutInternationalAccount internationalAccount);

    PayoutInternationalAccount getPayoutInternationalAccount(Long extPayoutId);

    Long savePayoutState(PayoutState payoutState);

    PayoutState getPayoutState(Long extPayoutId);

    Optional<Long> getLastEventId();

    Optional<LocalDateTime> getLastAggregationDate();

    void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo);

    List<PayoutAggsByHourRecord> getPayoutsAggsByHour(LocalDateTime dateFrom, LocalDateTime dateTo);

}
