package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.merch_stat.InvoicePaymentRefundStatus;
import com.rbkmoney.damsel.merch_stat.StatRequest;

import java.time.Instant;
import java.util.Optional;

public class DslUtil {

    public static StatRequest createPaymentsRequest(String partyId, String shopId, Instant fromTime, Instant toTime,
                                                    String continuationToken, int size, ObjectMapper objectMapper) {
        PaymentsForReportQuery paymentsQuery = new PaymentsForReportQuery();
        paymentsQuery.setMerchantId(partyId);
        paymentsQuery.setShopId(shopId);
        paymentsQuery.setFromTime(fromTime);
        paymentsQuery.setToTime(toTime);
        Query query = new Query();
        query.setPaymentsForReportQuery(paymentsQuery);
        query.setSize(size);
        StatisticDsl statisticDsl = new StatisticDsl();
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, continuationToken, objectMapper);
    }

    public static StatRequest createRefundsRequest(String partyId, String shopId, Instant fromTime, Instant toTime,
                                                   String continuationToken, int size, ObjectMapper objectMapper) {
        RefundsForReportQuery refundsForReportQuery = new RefundsForReportQuery();
        refundsForReportQuery.setMerchantId(partyId);
        refundsForReportQuery.setShopId(shopId);
        refundsForReportQuery.setFromTime(fromTime);
        refundsForReportQuery.setToTime(toTime);
        Query query = new Query();
        query.setRefundsForReportQuery(refundsForReportQuery);
        query.setSize(size);
        StatisticDsl statisticDsl = new StatisticDsl();
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, continuationToken, objectMapper);
    }

    public static StatRequest createAdjustmentsRequest(String partyId, String shopId, Instant fromTime, Instant toTime,
                                                       String continuationToken, int size, ObjectMapper objectMapper) {
        AdjustmentsForReportQuery adjustmentsForReportQuery = new AdjustmentsForReportQuery();
        adjustmentsForReportQuery.setMerchantId(partyId);
        adjustmentsForReportQuery.setShopId(shopId);
        adjustmentsForReportQuery.setFromTime(fromTime);
        adjustmentsForReportQuery.setToTime(toTime);
        Query query = new Query();
        query.setAdjustmentsForReportQuery(adjustmentsForReportQuery);
        query.setSize(size);
        StatisticDsl statisticDsl = new StatisticDsl();
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, continuationToken, objectMapper);
    }

    public static StatRequest createInvoicesRequest(String partyId, String shopId, Instant fromTime, Instant toTime,
                                                    String continuationToken, int size, ObjectMapper objectMapper) {
        InvoicesQuery invoicesQuery = new InvoicesQuery();
        invoicesQuery.setMerchantId(partyId);
        invoicesQuery.setShopId(shopId);
        invoicesQuery.setFromTime(fromTime);
        invoicesQuery.setToTime(toTime);
        Query query = new Query();
        query.setInvoicesQuery(invoicesQuery);
        query.setSize(size);
        StatisticDsl statisticDsl = new StatisticDsl();
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, continuationToken, objectMapper);
    }

    public static StatRequest createInvoiceRequest(String invoiceId, ObjectMapper objectMapper) {
        InvoicesQuery invoicesQuery = new InvoicesQuery();
        invoicesQuery.setInvoiceId(invoiceId);
        Query query = new Query();
        query.setInvoicesQuery(invoicesQuery);
        StatisticDsl statisticDsl = new StatisticDsl();
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, null, objectMapper);
    }

    public static StatRequest createPaymentRequest(String partyId, String shopId, String invoiceId, String paymentId,
                                                   ObjectMapper objectMapper) {
        PaymentsForReportQuery paymentsQuery = new PaymentsForReportQuery();
        paymentsQuery.setMerchantId(partyId);
        paymentsQuery.setShopId(shopId);
        paymentsQuery.setInvoiceId(invoiceId);
        paymentsQuery.setPaymentId(paymentId);
        Query query = new Query();
        query.setPaymentsForReportQuery(paymentsQuery);
        StatisticDsl statisticDsl = new StatisticDsl();
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, null, objectMapper);
    }

    public static StatRequest createStatRequest(StatisticDsl statisticDsl, String continuationToken,
                                                ObjectMapper objectMapper) {
        try {
            StatRequest statRequest = new StatRequest(objectMapper.writeValueAsString(statisticDsl));
            Optional.ofNullable(continuationToken)
                    .ifPresent(statRequest::setContinuationToken);
            return statRequest;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static StatRequest createShopAccountingStatRequest(String merchantId, String shopId, String currencyCode,
                                                              Optional<Instant> from, Instant to,
                                                              ObjectMapper objectMapper) {
        ShopAccountingQuery shopAccountingQuery = new ShopAccountingQuery();
        shopAccountingQuery.setMerchantId(merchantId);
        shopAccountingQuery.setShopId(shopId);
        shopAccountingQuery.setCurrencyCode(currencyCode);
        shopAccountingQuery.setFromTime(from);
        shopAccountingQuery.setToTime(to);
        Query query = new Query();
        query.setShopAccountingQuery(shopAccountingQuery);
        StatisticDsl statisticDsl = new StatisticDsl();
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, null, objectMapper);
    }

}
