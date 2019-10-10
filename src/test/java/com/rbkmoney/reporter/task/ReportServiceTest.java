package com.rbkmoney.reporter.task;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.dao.ReportDao;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.service.SignService;
import com.rbkmoney.reporter.service.StatisticService;
import com.rbkmoney.reporter.service.impl.TaskServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.thrift.TException;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.rbkmoney.reporter.util.TestDataUtil.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;

@Slf4j
public class ReportServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private TaskServiceImpl taskService;

    @Autowired
    private ReportDao reportDao;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private SignService signService;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private RepositoryClientSrv.Iface dominantClient;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    @Test
    public void generateProvisionOfServiceReportTest() throws IOException, TException, InterruptedException, DaoException {
        given(statisticService.getCapturedPaymentsIterator(anyString(), anyString(), any(), any())).willReturn(
                new Iterator<StatPayment>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public StatPayment next() {
                        return new StatPayment();
                    }
                }
        );

        given(statisticService.getRefundsIterator(anyString(), anyString(), any(), any())).willReturn(
                new Iterator<StatRefund>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public StatRefund next() {
                        return new StatRefund();
                    }
                }
        );

        String partyId = "TestPartyID";
        String shopId = "TestShopID";
        String contractId = random(String.class);
        Instant fromTime = random(Instant.class);
        Instant toTime = random(Instant.class);

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(getTestParty(partyId, shopId, contractId));
        given(partyManagementClient.getMetaData(any(), any(), any()))
                .willReturn(Value.b(true));
        ShopAccountingModel shopAccountingModel = random(ShopAccountingModel.class);
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(Instant.class)))
                .willReturn(shopAccountingModel);
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(), any(Instant.class)))
                .willReturn(shopAccountingModel);
        given(dominantClient.checkoutObject(any(), eq(Reference.payment_institution(new PaymentInstitutionRef(1)))))
                .willReturn(buildPaymentInstitutionObject(new PaymentInstitutionRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.calendar(new CalendarRef(1)))))
                .willReturn(buildPaymentCalendarObject(new CalendarRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.business_schedule(new BusinessScheduleRef(1)))))
                .willReturn(buildPayoutScheduleObject(new BusinessScheduleRef(1)));

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
            report = reportDao.getReport(partyId, shopId, reportId);
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
