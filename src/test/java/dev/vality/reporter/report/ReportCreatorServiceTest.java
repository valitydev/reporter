package dev.vality.reporter.report;

import dev.vality.damsel.domain_config.RepositoryClientSrv;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.reporter.config.PostgresqlSpringBootITest;
import dev.vality.reporter.dao.AdjustmentDao;
import dev.vality.reporter.dao.InvoiceDao;
import dev.vality.reporter.dao.PaymentDao;
import dev.vality.reporter.dao.RefundDao;
import dev.vality.reporter.data.ReportsTestData;
import dev.vality.reporter.domain.tables.pojos.Adjustment;
import dev.vality.reporter.domain.tables.pojos.Payment;
import dev.vality.reporter.domain.tables.pojos.Refund;
import dev.vality.reporter.domain.tables.pojos.Report;
import dev.vality.reporter.domain.tables.records.AdjustmentRecord;
import dev.vality.reporter.domain.tables.records.InvoiceRecord;
import dev.vality.reporter.domain.tables.records.PaymentRecord;
import dev.vality.reporter.domain.tables.records.RefundRecord;
import dev.vality.reporter.model.LocalReportCreatorDto;
import dev.vality.reporter.service.DominantService;
import dev.vality.reporter.service.LocalStatisticService;
import dev.vality.reporter.service.impl.LocalReportCreatorServiceImpl;
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

import static dev.vality.testcontainers.annotations.util.RandomBeans.random;
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

    @Autowired
    private DominantService dominantService;

    @MockBean
    private InvoiceDao invoiceDao;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;
    @MockBean
    private RepositoryClientSrv.Iface dominantClient;

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
        Map<String, String> shopNames = new HashMap<>();

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
                    shopNames
            );

            LocalReportCreatorServiceImpl reportCreatorServiceImpl =
                    new LocalReportCreatorServiceImpl(dominantService, statisticService);
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
