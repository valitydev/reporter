package com.rbkmoney.reporter.handler.report;

import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.ReportRequest;
import com.rbkmoney.reporter.ReportTimeRange;
import com.rbkmoney.reporter.StatReportRequest;
import com.rbkmoney.reporter.StatReportResponse;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.ReportNewProtoService;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static com.rbkmoney.testcontainers.annotations.util.RandomBeans.randomListOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.yml")
@DirtiesContext
@ContextConfiguration(classes = ReportsNewProtoHandler.class)
public class ReportHandlersTest {

    @MockBean
    private PartyService partyService;

    @MockBean
    private ReportNewProtoService reportNewProtoService;

    @Autowired
    private ReportsNewProtoHandler handler;

    @Test
    public void reportsNewProtoHandlerTest() throws TException {
        String partyId = "partyId";
        String shopId = "shopId";
        ReportType reportType = ReportType.payment_registry;
        List<String> shopIds = Collections.singletonList(shopId);
        Random random = new Random();

        when(partyService.getShop(anyString(), anyString())).thenReturn(new Shop());

        when(reportNewProtoService
                .createReport(eq(partyId), eq(shopId), any(), any(), eq(reportType)))
                .thenReturn(random.nextLong());

        LocalDateTime currMoment = LocalDateTime.now();
        IntStream.rangeClosed(1, 130).forEach(
                i -> {
                    try {
                        ReportTimeRange timeRange = new ReportTimeRange(
                                TypeUtil.temporalToString(currMoment.minusSeconds(i)),
                                TypeUtil.temporalToString(currMoment.plusSeconds(i)));

                        ReportRequest reportRequest = new ReportRequest()
                                .setPartyId(partyId)
                                .setShopId(shopId)
                                .setTimeRange(timeRange);

                        handler.createReport(reportRequest, reportType.getLiteral());
                    } catch (TException e) {
                        throw new RuntimeException();
                    }
                }
        );

        when(reportNewProtoService.getReportsWithToken(eq(partyId), eq(shopIds), any(), any(), any(), any(), eq(100)))
                .thenReturn(getReports(100));
        when(reportNewProtoService.getReportFiles(anyLong())).thenReturn(randomListOf(1, FileMeta.class));

        ReportRequest request = new ReportRequest()
                .setPartyId(partyId)
                .setShopIds(shopIds)
                .setTimeRange(
                        new ReportTimeRange(
                                TypeUtil.temporalToString(currMoment.minusDays(1)),
                                TypeUtil.temporalToString(currMoment.plusDays(1))
                        )
                );
        StatReportRequest statReportRequest = new StatReportRequest(request).setReportTypes(Collections.emptyList());
        StatReportResponse statReportResponseFirst = handler.getReports(statReportRequest);

        assertEquals(100, statReportResponseFirst.getReportsSize());
        assertNotNull(statReportResponseFirst.getContinuationToken());


        when(reportNewProtoService.getReportsWithToken(eq(partyId), eq(shopIds), any(), any(), any(), any(), eq(100)))
                .thenReturn(getReports(30));
        when(reportNewProtoService.getReportFiles(anyLong())).thenReturn(randomListOf(1, FileMeta.class));

        StatReportResponse statReportResponseSecond = handler.getReports(
                statReportRequest.setContinuationToken(statReportResponseFirst.getContinuationToken()));

        assertEquals(30, statReportResponseSecond.getReportsSize());
        assertNull(statReportResponseSecond.getContinuationToken());
    }

    private List<Report> getReports(int amount) {
        List<Report> reports = randomListOf(amount, Report.class);
        reports.forEach(report -> report.setStatus(ReportStatus.created));
        return reports;
    }
}
