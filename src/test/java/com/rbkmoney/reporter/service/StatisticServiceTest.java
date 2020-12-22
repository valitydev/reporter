package com.rbkmoney.reporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.merch_stat.MerchantStatisticsSrv;
import com.rbkmoney.damsel.merch_stat.StatRequest;
import com.rbkmoney.damsel.merch_stat.StatResponse;
import com.rbkmoney.damsel.merch_stat.StatResponseData;
import com.rbkmoney.reporter.config.AbstractStatisticServiceConfig;
import com.rbkmoney.reporter.dsl.DslUtil;
import com.rbkmoney.reporter.model.StatAdjustment;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class StatisticServiceTest extends AbstractStatisticServiceConfig {

    @MockBean
    private MerchantStatisticsSrv.Iface merchantStatisticsClient;

    @Autowired
    private StatisticService statisticService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreatePaymentRequest() {
        assertEquals(
                new StatRequest("{\"query\":{\"payments\":{\"merchant_id\":\"partyId\",\"shop_id\":\"shopId\",\"invoice_id\":\"invoiceId\",\"payment_id\":\"paymentId\"}}}"),
                DslUtil.createPaymentRequest("partyId", "shopId", "invoiceId", "paymentId", objectMapper)
        );
        assertEquals(
                new StatRequest("{\"query\":{\"size\":1000,\"payments_for_report\":{\"merchant_id\":\"partyId\",\"shop_id\":\"shopId\",\"from_time\":\"2018-10-28T09:15:00Z\",\"to_time\":\"2018-10-28T09:15:00Z\"}}}"),
                DslUtil.createPaymentsRequest(
                        "partyId",
                        "shopId",
                        LocalDateTime.of(2018, 10, 28, 9, 15).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2018, 10, 28, 9, 15).toInstant(ZoneOffset.UTC),
                        null,
                        1000,
                        objectMapper)
        );
    }

    @Test
    public void testValidate() throws TException {
        Map<String, String> statisticResponse = new HashMap<>();
        statisticResponse.put("merchant_id", "test");
        statisticResponse.put("shop_id", "test");
        statisticResponse.put("currency_code", null);
        statisticResponse.put("funds_acquired", "-10");
        statisticResponse.put("fee_charged", "-1");
        statisticResponse.put("funds_adjusted", "-1");
        statisticResponse.put("funds_paid_out", "-5");
        statisticResponse.put("funds_refunded", "-20");

        given(merchantStatisticsClient.getStatistics(any()))
                .willReturn(new StatResponse(StatResponseData.records(Arrays.asList(statisticResponse))));

        try {
            statisticService.getShopAccounting("test", "test", "RUB", Instant.now(), Instant.now());
            fail();
        } catch (ConstraintViolationException ex) {
            Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
            assertEquals(5, constraintViolations.size());
        }
    }

    @Test
    public void testAdjustmentIterator() throws TException {
        Map<String, String> adjustmentResponse = new HashMap<>();
        adjustmentResponse.put("payment_id", "1");
        adjustmentResponse.put("party_shop_id", "f6c216e0-e17f-41a7-b49d-60876a5182ce");
        adjustmentResponse.put("party_id", "6cf78788-16bc-486c-8bfd-48c5c1fda54a");
        adjustmentResponse.put("invoice_id", "1KUZZ795Zya");
        adjustmentResponse.put("id", "320894");
        adjustmentResponse.put("event_type", "INVOICE_PAYMENT_ADJUSTMENT_STATUS_CHANGED");
        adjustmentResponse.put("event_id", "0");
        adjustmentResponse.put("event_created_at", "2020-06-09T07:14:13.280809Z");
        adjustmentResponse.put("adjustment_status_created_at", "2020-06-09T07:14:13.277819Z");
        adjustmentResponse.put("adjustment_status", "captured");
        adjustmentResponse.put("adjustment_reason", "Изменение ставки");
        adjustmentResponse.put("adjustment_id", "1");
        adjustmentResponse.put("adjustment_domain_revision", "20029");
        adjustmentResponse.put("adjustment_currency_code", "RUB");
        adjustmentResponse.put("adjustment_created_at", "2020-06-09T07:10:56.239623Z");
        adjustmentResponse.put("adjustment_amount", "2500");

        given(merchantStatisticsClient.getStatistics(any()))
                .willReturn(new StatResponse(StatResponseData.records(Arrays.asList(adjustmentResponse))));

        Iterator<StatAdjustment> adjustmentIterator = statisticService.getAdjustmentsIterator("partyId", "shopId", Instant.MIN, Instant.MAX);
        assertTrue(adjustmentIterator.hasNext());
        StatAdjustment statAdjustment = adjustmentIterator.next();
        assertEquals(adjustmentResponse.get("invoice_id"), statAdjustment.getInvoiceId());
        assertEquals(adjustmentResponse.get("payment_id"), statAdjustment.getPaymentId());
        assertEquals(adjustmentResponse.get("adjustment_id"), statAdjustment.getAdjustmentId());
        assertEquals(adjustmentResponse.get("adjustment_reason"), statAdjustment.getAdjustmentReason());
        assertEquals(adjustmentResponse.get("adjustment_amount"), statAdjustment.getAdjustmentAmount().toString());
        assertEquals(adjustmentResponse.get("adjustment_created_at"), statAdjustment.getAdjustmentCreatedAt().toString());
        assertEquals(adjustmentResponse.get("adjustment_status_created_at"), statAdjustment.getAdjustmentStatusCreatedAt().toString());
    }
}
