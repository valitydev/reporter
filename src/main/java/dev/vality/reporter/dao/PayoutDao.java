package dev.vality.reporter.dao;

import dev.vality.reporter.domain.tables.pojos.Payout;
import dev.vality.reporter.domain.tables.pojos.PayoutAccount;
import dev.vality.reporter.domain.tables.pojos.PayoutInternationalAccount;
import dev.vality.reporter.domain.tables.pojos.PayoutState;

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
