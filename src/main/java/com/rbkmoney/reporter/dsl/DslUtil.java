package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.merch_stat.StatRequest;

import java.time.Instant;
import java.util.Collection;

public class DslUtil {

    public static StatRequest createPaymentsRequest(String partyId, String shopId, Instant from, Instant to, ObjectMapper objectMapper) {
        StatisticDsl statisticDsl = new StatisticDsl();
        Query query = new Query();
        PaymentsQuery paymentsQuery = new PaymentsQuery();
        paymentsQuery.setMerchantId(partyId);
        paymentsQuery.setShopId(shopId);
        paymentsQuery.setFromTime(from);
        paymentsQuery.setToTime(to);
        query.setPaymentsQuery(paymentsQuery);
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, objectMapper);
    }

    public static StatRequest createStatRequest(StatisticDsl statisticDsl, ObjectMapper objectMapper) {
        try {
            return new StatRequest(objectMapper.writeValueAsString(statisticDsl));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static StatRequest createShopAccountingStatRequest(Instant from, Instant to, ObjectMapper objectMapper) {
        StatisticDsl statisticDsl = new StatisticDsl();
        Query query = new Query();
        ShopAccountingQuery shopAccountingQuery = new ShopAccountingQuery();
        shopAccountingQuery.setFromTime(from);
        shopAccountingQuery.setToTime(to);
        query.setShopAccountingQuery(shopAccountingQuery);
        statisticDsl.setQuery(query);

        return createStatRequest(statisticDsl, objectMapper);
    }

}
