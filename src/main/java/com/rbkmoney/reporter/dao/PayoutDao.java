package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.tables.pojos.Payout;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutInternationalAccount;
import com.rbkmoney.reporter.domain.tables.pojos.PayoutState;

import java.util.Optional;

public interface PayoutDao {

    Long savePayout(Payout payout);

    void savePayoutAccountInfo(PayoutAccount payoutAccount);

    void savePayoutInternationalAccountInfo(PayoutInternationalAccount internationalAccount);

    Long savePayoutState(PayoutState payoutState);

    Optional<Long> getLastEventId();

}
