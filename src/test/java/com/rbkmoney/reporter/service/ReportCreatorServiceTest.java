package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.model.ReportCreatorDto;
import com.rbkmoney.reporter.model.StatAdjustment;
import com.rbkmoney.reporter.service.impl.ReportCreatorServiceImpl;
import com.rbkmoney.reporter.service.impl.StatisticServiceImpl;
import com.rbkmoney.reporter.util.BuildUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ReportCreatorServiceTest {

    @Test
    public void createBigSizeReportTest() throws IOException {
        StatisticService statisticsService = Mockito.mock(StatisticServiceImpl.class);
        when(statisticsService.getCapturedPayment(any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(BuildUtils.buildStatPayment(1));

        Iterator<StatPayment> paymentsIterator = IntStream.range(1, 10)
                .mapToObj(BuildUtils::buildStatPayment).iterator();

        Iterator<StatRefund> refundsIterator = IntStream.range(1, 10)
                .mapToObj(BuildUtils::buildStatRefund).iterator();

        Iterator<StatAdjustment> adjustmentsIterator = IntStream.range(1, 10)
                .mapToObj(BuildUtils::buildStatAdjustment).iterator();

        Report report = new Report();
        report.setTimezone("UTC");
        report.setPartyId("partyId");
        report.setPartyShopId("shopId");

        Map<String, String> shopUrls = new HashMap<>();
        Map<String, String> purposes = new HashMap<>();
        IntStream.range(1, 10).forEach(i -> {
            shopUrls.put("shopId" + i, "http://2ch.ru");
            purposes.put("invoiceId" + i, "Keksik");
        });

        Path tempFile = Files.createTempFile("check_limit", ".xlsx");
        try {
            ReportCreatorDto reportCreatorDto = new ReportCreatorDto(
                    "2019-03-22T06:12:27Z",
                    "2019-04-22T06:12:27Z",
                    paymentsIterator,
                    refundsIterator,
                    adjustmentsIterator,
                    report,
                    Files.newOutputStream(tempFile),
                    shopUrls,
                    purposes,
                    statisticsService
            );

            ReportCreatorServiceImpl reportCreatorServiceImpl = new ReportCreatorServiceImpl();
            reportCreatorServiceImpl.setLimit(10);
            reportCreatorServiceImpl.createReport(reportCreatorDto);
            Workbook wb = new XSSFWorkbook(Files.newInputStream(tempFile));
            assertNotNull(wb.getSheetAt(0));
            assertNotNull(wb.getSheetAt(1));
            assertNotNull(wb.getSheetAt(2));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
