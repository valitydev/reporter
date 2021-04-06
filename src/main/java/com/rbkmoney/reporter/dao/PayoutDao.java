package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutInternationalAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PayoutDao {

    Long savePayout(Payout payout);

    Payout getPayout(String payoutId);

    void savePayoutAccountInfo(PayoutAccount payoutAccount);

    PayoutAccount getPayoutAccount(Long extPayoutId);

    void savePayoutInternationalAccountInfo(PayoutInternationalAccount internationalAccount);

    PayoutInternationalAccount getPayoutInternationalAccount(Long extPayoutId);

    Long savePayoutState(PayoutState payoutState);

    PayoutState getPayoutState(Long extPayoutId);

    Optional<Long> getLastEventId();

    Long getFundsPayOutAmount(String partyId,
                              String partyShopId,
                              String currencyCode,
                              Optional<LocalDateTime> fromTime,
                              LocalDateTime toTime);

}
