package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.reporter.data.InvoicingData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DamselUtilTest {

    @Test
    public void testComputeAdjustmentAmount() {
        List<FinalCashFlowPosting> paymentCashFlow = InvoicingData.createPaymentCashFlow();
        List<FinalCashFlowPosting> oldCashFlow = InvoicingData.createOldCashFlow();
        List<FinalCashFlowPosting> newCashFlow = InvoicingData.createNewCashFlow();

        long paymentAmount = DamselUtil.computeMerchantAmount(paymentCashFlow);
        long oldAmount = DamselUtil.computeMerchantAmount(oldCashFlow);
        long newAmount = DamselUtil.computeMerchantAmount(newCashFlow);

        assertEquals(paymentAmount, -oldAmount);
        assertEquals(2418, newAmount + oldAmount);
    }

    @Test
    public void testAdjustmentStatusChangeCaptureToFailed() {
        List<FinalCashFlowPosting> paymentCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> oldCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> newCashFlow = new ArrayList<>();

        long paymentAmount = DamselUtil.computeMerchantAmount(paymentCashFlow);
        long oldAmount = DamselUtil.computeMerchantAmount(oldCashFlow);
        long newAmount = DamselUtil.computeMerchantAmount(newCashFlow);

        assertEquals(0, newAmount);
        assertEquals(-paymentAmount, newAmount + oldAmount);
    }

    @Test
    public void testAdjustmentStatusChangeFailedToCapture() {
        List<FinalCashFlowPosting> paymentCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> oldCashFlow = new ArrayList<>();

        List<FinalCashFlowPosting> newCashFlow = List.of(
                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        long paymentAmount = DamselUtil.computeMerchantAmount(paymentCashFlow);
        long oldAmount = DamselUtil.computeMerchantAmount(oldCashFlow);
        long newAmount = DamselUtil.computeMerchantAmount(newCashFlow);

        assertEquals(paymentAmount, newAmount + oldAmount);
    }
}
