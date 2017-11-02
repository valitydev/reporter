package com.rbkmoney.reporter.task;

import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.ReportType;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.model.PartyModel;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.service.SignService;
import com.rbkmoney.reporter.service.StatisticService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

public class ReportServiceTest extends AbstractIntegrationTest {

    @Autowired
    ReportService reportService;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private PartyService partyService;

    @MockBean
    private SignService signService;

    @Before
    public void setup() {
        given(statisticService.getPayments(anyString(), anyString(), any(), any())).willReturn(new ArrayList<>());
        given(partyService.getPartyRepresentation(anyString(), anyString(), any(Instant.class))).willReturn(random(PartyModel.class));
        given(signService.sign(any(Path.class)))
                .willAnswer(
                        (Answer<byte[]>) invocation -> Base64.getEncoder().encode(Files.readAllBytes(invocation.getArgumentAt(0, Path.class)))
                );
    }

    @Test
    public void generateProvisionOfServiceReportTest() throws IOException {
        ShopAccountingModel shopAccountingModel = random(ShopAccountingModel.class);
        given(statisticService.getShopAccounting(anyString(), anyString(), any(Instant.class), any(Instant.class))).willReturn(shopAccountingModel);

        String partyId = random(String.class);
        String shopId = random(String.class);
        Instant fromTime = random(Instant.class);
        Instant toTime = random(Instant.class);
        ReportType reportType = ReportType.provision_of_service;

        long reportId = reportService.createReport(partyId, shopId, fromTime, toTime, reportType);

        Report report;
        do {
            report = reportService.getReport(partyId, shopId, reportId);
        } while (report.getStatus() != ReportStatus.created);

        List<FileMeta> reportFiles = reportService.getReportFiles(report.getId());
        assertEquals(2, reportFiles.size());
        for (FileMeta fileMeta : reportFiles) {
            URL url = reportService.generatePresignedUrl(fileMeta.getFileId(), LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));
            assertNotNull(url);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (InputStream inputStream = url.openStream()) {
                    Streams.copy(inputStream, outputStream, true);
                    byte[] actualBytes = outputStream.toByteArray();
                    assertEquals(fileMeta.getMd5(), DigestUtils.md5Hex(actualBytes));
                    assertEquals(fileMeta.getSha256(), DigestUtils.sha256Hex(actualBytes));
                }
            }
        }
    }

}
