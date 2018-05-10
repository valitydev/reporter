package com.rbkmoney.reporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.dsl.DslUtil;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

public class StatisticServiceTest extends AbstractIntegrationTest {

    @Autowired
    StatisticService statisticService;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    MerchantStatisticsSrv.Iface merchantStatisticsClient;

    @Test
    public void testCreatePaymentRequest() {
        assertEquals(
                new StatRequest("{\"query\":{\"payments\":{\"invoice_id\":\"invoiceId\",\"payment_id\":\"paymentId\",\"payment_status\":\"captured\"}}}"),
                DslUtil.createPaymentRequest("invoiceId", "paymentId", Optional.of(InvoicePaymentStatus.captured(new InvoicePaymentCaptured())), objectMapper)
        );
        assertEquals(
                new StatRequest("{\"query\":{\"payments\":{\"invoice_id\":\"invoiceId\",\"payment_id\":\"paymentId\"}}}"),
                DslUtil.createPaymentRequest("invoiceId", "paymentId", Optional.empty(), objectMapper)
        );
    }

    @Test
    public void testValidate() throws TException {
        Map<String, String> statisticResponse = new HashMap<>();
        statisticResponse.put("merchant_id", "test");
        statisticResponse.put("contract_id", "test");
        statisticResponse.put("currency_code", null);
        statisticResponse.put("funds_acquired", "-10");
        statisticResponse.put("fee_charged", "-1");
        statisticResponse.put("funds_adjusted", "-1");
        statisticResponse.put("funds_paid_out", "-5");
        statisticResponse.put("funds_refunded", "-20");

        given(merchantStatisticsClient.getStatistics(new StatRequest(anyString())))
                .willReturn(new StatResponse(StatResponseData.records(Arrays.asList(statisticResponse))));

        try {
            statisticService.getShopAccounting("test", "test", "RUB", Instant.now(), Instant.now());
            fail();
        } catch (ConstraintViolationException ex) {
            Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
            assertEquals(5, constraintViolations.size());
        }
    }

}
