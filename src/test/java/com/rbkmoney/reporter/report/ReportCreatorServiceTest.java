package com.rbkmoney.reporter.report;

import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.reporter.config.PostgresqlSpringBootITest;
import com.rbkmoney.reporter.dao.AdjustmentDao;
import com.rbkmoney.reporter.dao.InvoiceDao;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.dao.RefundDao;
import com.rbkmoney.reporter.data.ReportsTestData;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.reporter.domain.tables.records.InvoiceRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.rbkmoney.reporter.model.LocalReportCreatorDto;
import com.rbkmoney.reporter.service.LocalStatisticService;
import com.rbkmoney.reporter.service.impl.LocalReportCreatorServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.Cursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static com.rbkmoney.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@PostgresqlSpringBootITest
public class ReportCreatorServiceTest {

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @Autowired
    private LocalStatisticService statisticService;

    @MockBean
    private InvoiceDao invoiceDao;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    private String partyId;
    private String shopId;

    @BeforeEach
    public void setUp() {
        InvoiceRecord invoiceRecord = new InvoiceRecord();
        invoiceRecord.setProduct("rbk.money");
        Mockito.when(invoiceDao.getInvoice(any())).thenReturn(invoiceRecord);
        partyId = random(String.class);
        shopId = random(String.class);
        int defaultOperationsCount = 100;

        List<PaymentRecord> paymentRecordList = new ArrayList<>();
        for (int i = 0; i < defaultOperationsCount; ++i) {
            LocalDateTime createdAt = LocalDateTime.now().minusHours(defaultOperationsCount + 1 - i);
            PaymentRecord paymentRecord = ReportsTestData.buildPaymentRecord(i, partyId, shopId, createdAt);
            paymentRecordList.add(paymentRecord);
            paymentDao.savePayment(paymentRecord.into(Payment.class));
        }

        List<RefundRecord> refundRecordList = new ArrayList<>();
        for (int i = 0; i < defaultOperationsCount; ++i) {
            LocalDateTime createdAt = LocalDateTime.now().minusHours(defaultOperationsCount + 1 - i);
            RefundRecord refundRecord =
                    ReportsTestData.buildRefundRecord(i, partyId, shopId, 123L + i, createdAt);
            refundRecordList.add(refundRecord);
            refundDao.saveRefund(refundRecord.into(Refund.class));
        }

        List<AdjustmentRecord> adjustmentRecordList = new ArrayList<>();
        for (int i = 0; i < defaultOperationsCount; ++i) {
            LocalDateTime createdAt = LocalDateTime.now().minusHours(defaultOperationsCount + 1 - i);
            AdjustmentRecord adjustmentRecord =
                    ReportsTestData.buildAdjustmentRecord(i, partyId, shopId, 123L + i, createdAt);
            adjustmentRecordList.add(adjustmentRecord);
            adjustmentDao.saveAdjustment(adjustmentRecord.into(Adjustment.class));
        }
    }

    @Test
    public void createBigSizeReportTest() throws IOException {
        LocalDateTime fromTime = LocalDateTime.now().minusHours(128L);
        LocalDateTime toTime = LocalDateTime.now();
        Cursor<PaymentRecord> paymentCursor =
                paymentDao.getPaymentsCursor(partyId, shopId, Optional.of(fromTime), toTime);
        Cursor<RefundRecord> refundCursor =
                refundDao.getRefundsCursor(partyId, shopId, fromTime, toTime);
        Cursor<AdjustmentRecord> adjustmentCursor =
                adjustmentDao.getAdjustmentCursor(partyId, shopId, fromTime, toTime);

        Report report = new Report();
        report.setTimezone("UTC");
        report.setPartyId(partyId);
        report.setPartyShopId(shopId);

        Map<String, String> shopUrls = new HashMap<>();
        Map<String, String> purposes = new HashMap<>();
        IntStream.range(1, 10).forEach(i -> {
            shopUrls.put("shopId" + i, "http://2ch.ru");
            purposes.put("invoiceId" + i, "Keksik");
        });

        Path tempFile = Files.createTempFile("check_limit", ".xlsx");
        try {
            LocalReportCreatorDto reportCreatorDto = new LocalReportCreatorDto(
                    fromTime.toString(),
                    toTime.toString(),
                    paymentCursor,
                    refundCursor,
                    adjustmentCursor,
                    report,
                    Files.newOutputStream(tempFile),
                    shopUrls,
                    purposes
            );

            LocalReportCreatorServiceImpl reportCreatorServiceImpl =
                    new LocalReportCreatorServiceImpl(statisticService);
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
