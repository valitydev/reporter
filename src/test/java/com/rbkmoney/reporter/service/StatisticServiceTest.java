package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.merch_stat.MerchantStatisticsSrv;
import com.rbkmoney.damsel.merch_stat.StatRequest;
import com.rbkmoney.damsel.merch_stat.StatResponse;
import com.rbkmoney.damsel.merch_stat.StatResponseData;
import com.rbkmoney.reporter.AbstractIntegrationTest;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

public class StatisticServiceTest extends AbstractIntegrationTest {

    @Autowired
    StatisticService statisticService;

    @MockBean
    DomainConfigService domainConfigService;

    @MockBean
    MerchantStatisticsSrv.Iface merchantStatisticsClient;

    @Test
    public void testValidate() throws TException {
        Map<String, String> statisticResponse = new HashMap<>();
        statisticResponse.put("merchant_id", "test");
        statisticResponse.put("shop_id", "test");
        statisticResponse.put("currency_code", null);
        statisticResponse.put("funds_acquired", "-10");
        statisticResponse.put("fee_charged", "-1");
        statisticResponse.put("opening_balance", "-15");
        statisticResponse.put("funds_paid_out", "-5");
        statisticResponse.put("funds_refunded", "-20");
        statisticResponse.put("closing_balance", "-34");

        given(merchantStatisticsClient.getStatistics(new StatRequest(anyString())))
                .willReturn(new StatResponse(StatResponseData.records(Arrays.asList(statisticResponse))));
        given(domainConfigService.getTestCategories()).willReturn(new HashMap<>());

        try {
            statisticService.getShopAccounting("test", "test", Instant.now(), Instant.now());
            fail();
        } catch (ConstraintViolationException ex) {
            Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
            assertEquals(7, constraintViolations.size());
        }
    }

}
