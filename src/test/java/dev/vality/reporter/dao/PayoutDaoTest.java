package dev.vality.reporter.dao;

import dev.vality.reporter.config.PostgresqlSpringBootITest;
import dev.vality.reporter.domain.tables.pojos.Payout;
import dev.vality.reporter.domain.tables.pojos.PayoutAccount;
import dev.vality.reporter.domain.tables.pojos.PayoutInternationalAccount;
import dev.vality.reporter.domain.tables.pojos.PayoutState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static dev.vality.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class PayoutDaoTest {

    @Autowired
    private PayoutDao payoutDao;

    @Test
    public void payoutGetTest() {
        Payout payout = random(Payout.class);
        payoutDao.savePayout(payout);
        Payout resultPayout = payoutDao.getPayout(payout.getPayoutId());
        assertEquals(payout, resultPayout);

        Long extPayoutId = resultPayout.getId();
        PayoutAccount payoutAccount = random(PayoutAccount.class);
        payoutAccount.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutAccountInfo(payoutAccount);
        PayoutAccount resultPayoutAccount = payoutDao.getPayoutAccount(extPayoutId);
        assertEquals(payoutAccount, resultPayoutAccount);

        PayoutInternationalAccount payoutInternationalAccount = random(PayoutInternationalAccount.class);
        payoutInternationalAccount.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutInternationalAccountInfo(payoutInternationalAccount);
        PayoutInternationalAccount internationalAccount = payoutDao.getPayoutInternationalAccount(extPayoutId);
        assertEquals(payoutInternationalAccount, internationalAccount);

        PayoutState payoutState = random(PayoutState.class);
        payoutState.setExtPayoutId(extPayoutId);
        payoutDao.savePayoutState(payoutState);
        PayoutState resultPayoutState = payoutDao.getPayoutState(extPayoutId);
        assertEquals(payoutState, resultPayoutState);
    }
}
