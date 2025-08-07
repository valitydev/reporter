package dev.vality.reporter.service;

import dev.vality.damsel.domain_config_v2.RepositoryClientSrv;
import dev.vality.reporter.dao.ReportDao;
import dev.vality.reporter.domain.enums.ReportStatus;
import dev.vality.reporter.domain.enums.ReportType;
import dev.vality.reporter.domain.tables.pojos.FileMeta;
import dev.vality.reporter.domain.tables.pojos.Report;
import dev.vality.testcontainers.annotations.minio.MinioTestcontainerSingleton;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.vality.reporter.data.CommonTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@PostgresqlTestcontainer
@MinioTestcontainerSingleton
class GenerateReportIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDao reportDao;

    @MockitoBean
    private RepositoryClientSrv.Iface repositoryClient;

    @Test
    void generatePaymentRegistryReportTest() throws Exception {
        String partyId = "TestPartyID";
        String shopId = "TestShopID";
        Instant fromTime = LocalDateTime.now().minusHours(10L).toInstant(ZoneOffset.UTC);
        Instant toTime = Instant.now();

        given(repositoryClient.checkoutObject(any(), any()))
                .willReturn(getVersionedObject(getTestParty(partyId, shopId)));
        given(repositoryClient.checkoutObjects(any(), any()))
                .willReturn(List.of(getVersionedObject(getTestShop(shopId))));

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
