package com.rbkmoney.reporter;

import com.rbkmoney.damsel.domain.ArticlesOfAssociation;
import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.damsel.domain.Representative;
import com.rbkmoney.damsel.domain.RepresentativeDocument;
import com.rbkmoney.reporter.config.AbstractIntegrationConfig;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.service.impl.TaskServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.thrift.TException;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class GenerateReportIntegrationTest extends AbstractIntegrationConfig {

    @Autowired
    private ReportService reportService;

    @Autowired
    private TaskServiceImpl taskService;

    @Autowired
    private ReportDao reportDao;

    @Test
    public void generateProvisionOfServiceReportTest() throws IOException, TException, InterruptedException, DaoException {
        taskService.registerProvisionOfServiceJob(
                partyId,
                contractId,
                1L,
                new BusinessScheduleRef(1),
                new Representative("test", "test", RepresentativeDocument.articles_of_association(new ArticlesOfAssociation()))
        );

        ReportType reportType = ReportType.provision_of_service;

        long reportId = reportService.createReport(partyId, shopId, fromTime, toTime, reportType);

        Report report;
        int retryCount = 0;
        do {
            TimeUnit.SECONDS.sleep(5L);
            report = reportDao.getReport(reportId);
            retryCount++;
        } while (report != null && report.getStatus() != ReportStatus.created && retryCount <= 10);

        assertEquals(ReportStatus.created, report == null ? null : report.getStatus());
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
