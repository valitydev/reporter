package com.rbkmoney.reporter.handler;

import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.*;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.exception.DaoException;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.TimeZone;
import java.util.stream.IntStream;

import static com.rbkmoney.reporter.util.TestDataUtil.getTestParty;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class ReportsNewProtoHandlerTest extends AbstractIntegrationTest {

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    @Autowired
    private ReportsNewProtoHandler handler;

    @Test
    public void testGetReports() throws TException {

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(getTestParty("partyId", "shopId", "contractId"));

        LocalDateTime currMoment = LocalDateTime.now();
        IntStream.rangeClosed(1, 130).forEach(i -> {
            try {
                handler.createReport(new ReportRequest()
                        .setPartyId("partyId")
                        .setShopId("shopId")
                        .setTimeRange(new ReportTimeRange(
                        TypeUtil.temporalToString(currMoment.minusSeconds(i)),
                        TypeUtil.temporalToString(currMoment.plusSeconds(i)))), "provision_of_service");
            } catch (TException e) {
                throw new RuntimeException();
            }
        });

        ReportRequest request = new ReportRequest()
                .setPartyId("partyId")
                .setShopId("shopId")
                .setTimeRange(new ReportTimeRange(
                        TypeUtil.temporalToString(currMoment.minusDays(1)),
                        TypeUtil.temporalToString(currMoment.plusDays(1))));
        StatReportRequest statReportRequest = new StatReportRequest(request).setReportTypes(Collections.emptyList());
        StatReportResponse statReportResponseFirst = handler.getReports(statReportRequest);
        assertEquals(100, statReportResponseFirst.getReportsSize());
        assertNotNull(statReportResponseFirst.getContinuationToken());
        StatReportResponse statReportResponseSecond = handler.getReports(statReportRequest.setContinuationToken(statReportResponseFirst.getContinuationToken()));
        assertEquals(30, statReportResponseSecond.getReportsSize());
        assertNull(statReportResponseSecond.getContinuationToken());
    }
}
