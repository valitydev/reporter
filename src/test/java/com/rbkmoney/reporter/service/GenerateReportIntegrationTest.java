package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.reporter.config.AbstractIntegrationConfig;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.rbkmoney.reporter.data.CommonTestData.getTestParty;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Slf4j
public class GenerateReportIntegrationTest extends AbstractIntegrationConfig {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDao reportDao;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    @Test
    public void generatePaymentRegistryReportTest() throws Exception {
        String partyId = "TestPartyID";
        String shopId = "TestShopID";
        Instant fromTime = LocalDateTime.now().minusHours(10L).toInstant(ZoneOffset.UTC);
        Instant toTime = Instant.now();

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(getTestParty(partyId, shopId, random(String.class)));
        given(partyManagementClient.getMetaData(any(), any(), any()))
                .willReturn(Value.b(true));

        ReportType reportType = ReportType.payment_registry;
        long reportId = reportService.createReport(partyId, shopId, fromTime, toTime, reportType);

        Report report;
        int retryCount = 0;
        do {
            TimeUnit.SECONDS.sleep(5L);
            report = reportDao.getReport(reportId);
            retryCount++;
        } while (report != null && report.getStatus() != ReportStatus.created && retryCount <= 10);

        assertNotNull(report);
        assertEquals(ReportStatus.created, report.getStatus());

        List<FileMeta> reportFiles = reportService.getReportFiles(report.getId());
        assertEquals(1, reportFiles.size());
        for (FileMeta fileMeta : reportFiles) {
            URL url = reportService.generatePresignedUrl(fileMeta.getFileId(),
                    LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));
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
