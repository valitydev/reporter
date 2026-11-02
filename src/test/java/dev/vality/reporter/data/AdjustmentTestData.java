package dev.vality.reporter.data;

import dev.vality.damsel.domain.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdjustmentTestData {

    public static InvoicePaymentAdjustment createAdjustment(String id,
                                                             InvoicePaymentAdjustmentState state,
                                                             List<FinalCashFlowPosting> oldCashFlowInverse,
                                                             List<FinalCashFlowPosting> newCashFlow) {
        return new InvoicePaymentAdjustment(id,
                InvoicePaymentAdjustmentStatus.captured(new InvoicePaymentAdjustmentCaptured()),
                "2026-09-24T12:20:32Z", 1L, "test", newCashFlow, oldCashFlowInverse).setState(state);
    }

    public static InvoicePaymentAdjustmentState cashFlowState() {
        return InvoicePaymentAdjustmentState.cash_flow(
                new InvoicePaymentAdjustmentCashFlowState(new InvoicePaymentAdjustmentCashFlow()));
    }

    public static InvoicePaymentAdjustmentState statusChangeState(InvoicePaymentStatus targetStatus) {
        return InvoicePaymentAdjustmentState.status_change(new InvoicePaymentAdjustmentStatusChangeState(
                new InvoicePaymentAdjustmentStatusChange(targetStatus)));
    }

    public static InvoicePaymentStatus capturedStatus() {
        return InvoicePaymentStatus.captured(new InvoicePaymentCaptured());
    }

    public static InvoicePaymentStatus failedStatus() {
        return InvoicePaymentStatus.failed(new InvoicePaymentFailed());
    }

    public static List<FinalCashFlowPosting> cashFlow(long amount, long fee, long providerFee) {
        var merchant = CashFlowAccount.merchant(MerchantCashFlowAccount.settlement);
        var provider = CashFlowAccount.provider(ProviderCashFlowAccount.settlement);
        var system = CashFlowAccount.system(SystemCashFlowAccount.settlement);
        return List.of(posting(provider, merchant, amount), posting(merchant, system, fee),
                posting(system, provider, providerFee));
    }

    public static FinalCashFlowPosting posting(CashFlowAccount source, CashFlowAccount destination, long amount) {
        return new FinalCashFlowPosting()
                .setSource(new FinalCashFlowAccount().setAccountType(source))
                .setDestination(new FinalCashFlowAccount().setAccountType(destination))
                .setVolume(new Cash(amount, new CurrencyRef("RUB")));
    }
}
