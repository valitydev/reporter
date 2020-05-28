package com.rbkmoney.reporter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payout_processing.PayoutSummaryItem;
import com.rbkmoney.damsel.reports.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import org.apache.thrift.TBase;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DamselUtil {

    public static Report toDamselReport(com.rbkmoney.reporter.domain.tables.pojos.Report report, List<com.rbkmoney.reporter.domain.tables.pojos.FileMeta> files) throws IllegalArgumentException {
        Report dReport = new Report();
        dReport.setReportId(report.getId());
        dReport.setStatus(ReportStatus.valueOf(report.getStatus().getLiteral()));
        ReportTimeRange timeRange = new ReportTimeRange(
                TypeUtil.temporalToString(report.getFromTime()),
                TypeUtil.temporalToString(report.getToTime())
        );
        dReport.setTimeRange(timeRange);
        dReport.setReportType(ReportType.valueOf(report.getType().name()));
        dReport.setCreatedAt(TypeUtil.temporalToString(report.getCreatedAt()));

        dReport.setFiles(files.stream()
                .map(DamselUtil::toDamselFile)
                .collect(Collectors.toList()));

        return dReport;
    }

    public static FileMeta toDamselFile(com.rbkmoney.reporter.domain.tables.pojos.FileMeta file) {
        FileMeta fileMeta = new FileMeta();
        fileMeta.setFileId(file.getFileId());
        fileMeta.setFilename(file.getFilename());
        Signature signature = new Signature();
        signature.setMd5(file.getMd5());
        signature.setSha256(file.getSha256());
        fileMeta.setSignature(signature);
        return fileMeta;
    }

    public static InvalidRequest buildInvalidRequest(Throwable throwable) {
        return buildInvalidRequest(throwable.getMessage());
    }

    public static InvalidRequest buildInvalidRequest(String... messages) {
        return new InvalidRequest(Arrays.asList(messages));
    }

    public static String toPayoutSummaryStatString(
            List<com.rbkmoney.damsel.payout_processing.PayoutSummaryItem> payoutSummaryItems
    ) {
        try {
            return new ObjectMapper().writeValueAsString(convertJsonFromPayoutSummary(payoutSummaryItems));
        } catch (IOException ex) {
            throw new RuntimeJsonMappingException(ex.getMessage());
        }
    }

    private static List<JsonNode> convertJsonFromPayoutSummary(List<PayoutSummaryItem> payoutSummaryItems) {
        return payoutSummaryItems.stream()
                .map(
                        payoutSummaryItem -> {
                            try {
                                return new TBaseProcessor().process(payoutSummaryItem, new JsonHandler());
                            } catch (IOException ex) {
                                throw new RuntimeJsonMappingException(ex.getMessage());
                            }
                        }
                ).collect(Collectors.toList());
    }

    public static String toJsonString(TBase tBase) {
        return toJson(tBase).toString();
    }

    public static JsonNode toJson(TBase tBase) {
        try {
            return new TBaseProcessor().process(tBase, new JsonHandler());
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

    public static String getCurrencyFromCashFlow(List<FinalCashFlowPosting> finalCashFlow) {
        return finalCashFlow.stream()
                .filter(cashtFlow -> cashtFlow.isSetVolume() && cashtFlow.getVolume().isSetCurrency())
                .map(cashtFlow -> cashtFlow.getVolume().getCurrency().getSymbolicCode())
                .findFirst()
                .orElse(null);
    }

    private static long computeAmount(List<FinalCashFlowPosting> finalCashFlow,
                                      Function<FinalCashFlowPosting, FinalCashFlowAccount> func) {
        return finalCashFlow.stream()
                .filter(f -> isMerchantSettlement(func.apply(f).getAccountType()))
                .mapToLong(cashFlow -> cashFlow.getVolume().getAmount())
                .sum();
    }

    private static boolean isMerchantSettlement(CashFlowAccount cashFlowAccount) {
        return cashFlowAccount.isSetMerchant() &&
                cashFlowAccount.getMerchant() == MerchantCashFlowAccount.settlement;
    }

}
