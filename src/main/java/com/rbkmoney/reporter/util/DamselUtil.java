package com.rbkmoney.reporter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.reporter.model.FeeType;
import org.apache.thrift.TBase;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DamselUtil {

    public static String toJsonString(TBase base) {
        return toJson(base).toString();
    }

    public static JsonNode toJson(TBase base) {
        try {
            return new TBaseProcessor().process(base, new JsonHandler());
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static String getInvoiceStatusDetails(com.rbkmoney.damsel.domain.InvoiceStatus invoiceStatus) {
        switch (invoiceStatus.getSetField()) {
            case FULFILLED:
                return invoiceStatus.getFulfilled().getDetails();
            case CANCELLED:
                return invoiceStatus.getCancelled().getDetails();
            default:
                return null;
        }
    }

    public static Map<FeeType, Long> getFees(List<FinalCashFlowPosting> cashFlowPostings) {
        if (cashFlowPostings != null && !cashFlowPostings.isEmpty()) {
            return cashFlowPostings.stream()
                    .collect(
                            Collectors.groupingBy(
                                    DamselUtil::getFeeType,
                                    Collectors.summingLong(posting -> posting.getVolume().getAmount())
                            )
                    );
        } else {
            return Map.of();
        }
    }

    public static Map<FeeType, String> getCurrency(List<FinalCashFlowPosting> cashFlowPostings) {
        if (cashFlowPostings != null && !cashFlowPostings.isEmpty()) {
            return cashFlowPostings.stream()
                    .collect(
                            Collectors.groupingBy(
                                    DamselUtil::getFeeType,
                                    Collectors.mapping(
                                            o -> o.getVolume().getCurrency().getSymbolicCode(),
                                            Collectors.collectingAndThen(
                                                    Collectors.toList(),
                                                    values -> values.isEmpty() ? null : values.get(0)
                                            )
                                    )
                            )
                    );
        } else {
            return Map.of();
        }
    }

    public static FeeType getFeeType(FinalCashFlowPosting cashFlowPosting) {
        CashFlowAccount source = cashFlowPosting.getSource().getAccountType();
        CashFlowAccount destination = cashFlowPosting.getDestination().getAccountType();

        if (source.isSetProvider() && source.getProvider() == ProviderCashFlowAccount.settlement
                && destination.isSetMerchant() && destination.getMerchant() == MerchantCashFlowAccount.settlement) {
            return FeeType.AMOUNT;
        }

        if (source.isSetMerchant()
                && source.getMerchant() == MerchantCashFlowAccount.settlement
                && destination.isSetSystem()) {
            return FeeType.FEE;
        }

        if (source.isSetSystem()
                && destination.isSetExternal()) {
            return FeeType.EXTERNAL_FEE;
        }

        if (source.isSetSystem()
                && destination.isSetProvider()) {
            return FeeType.PROVIDER_FEE;
        }

        return FeeType.UNKNOWN;
    }

    public static Long computeMerchantAmount(List<FinalCashFlowPosting> finalCashFlow) {
        long amountSource = computeAmount(finalCashFlow, FinalCashFlowPosting::getSource);
        long amountDest = computeAmount(finalCashFlow, FinalCashFlowPosting::getDestination);
        return amountDest - amountSource;
    }

    private static long computeAmount(List<FinalCashFlowPosting> finalCashFlow,
                                      Function<FinalCashFlowPosting, FinalCashFlowAccount> func) {
        return finalCashFlow.stream()
                .filter(f -> isMerchantSettlement(func.apply(f).getAccountType()))
                .mapToLong(cashFlow -> cashFlow.getVolume().getAmount())
                .sum();
    }

    private static boolean isMerchantSettlement(CashFlowAccount cashFlowAccount) {
        return cashFlowAccount.isSetMerchant()
                && cashFlowAccount.getMerchant() == MerchantCashFlowAccount.settlement;
    }

    public static String getCurrencyFromCashFlow(List<FinalCashFlowPosting> finalCashFlow) {
        return finalCashFlow.stream()
                .filter(cashtFlow -> cashtFlow.isSetVolume() && cashtFlow.getVolume().isSetCurrency())
                .map(cashtFlow -> cashtFlow.getVolume().getCurrency().getSymbolicCode())
                .findFirst()
                .orElse(null);
    }

}
