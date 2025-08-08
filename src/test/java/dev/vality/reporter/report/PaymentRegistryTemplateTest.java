package dev.vality.reporter.report;

import dev.vality.damsel.domain.Currency;
import dev.vality.damsel.domain.CurrencyObject;
import dev.vality.damsel.domain.CurrencyRef;
import dev.vality.damsel.domain.DomainObject;
import dev.vality.damsel.domain_config_v2.RepositoryClientSrv;
import dev.vality.damsel.domain_config_v2.VersionedObject;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.reporter.config.PostgresqlSpringBootITest;
import dev.vality.reporter.dao.AdjustmentDao;
import dev.vality.reporter.dao.InvoiceDao;
import dev.vality.reporter.dao.PaymentDao;
import dev.vality.reporter.dao.RefundDao;
import dev.vality.reporter.data.ReportsTestData;
import dev.vality.reporter.domain.enums.InvoiceStatus;
import dev.vality.reporter.domain.enums.RefundStatus;
import dev.vality.reporter.domain.tables.pojos.Adjustment;
import dev.vality.reporter.domain.tables.pojos.Payment;
import dev.vality.reporter.domain.tables.pojos.Refund;
import dev.vality.reporter.domain.tables.pojos.Report;
import dev.vality.reporter.domain.tables.records.AdjustmentRecord;
import dev.vality.reporter.domain.tables.records.InvoiceRecord;
import dev.vality.reporter.domain.tables.records.PaymentRecord;
import dev.vality.reporter.domain.tables.records.RefundRecord;
import dev.vality.reporter.template.LocalPaymentRegistryTemplateImpl;
import dev.vality.reporter.util.FormatUtil;
import dev.vality.reporter.util.TimeUtil;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static dev.vality.reporter.data.CommonTestData.*;
import static dev.vality.reporter.domain.enums.InvoicePaymentStatus.captured;
import static dev.vality.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@PostgresqlSpringBootITest
class PaymentRegistryTemplateTest {

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @Autowired
    private LocalPaymentRegistryTemplateImpl paymentRegistryTemplate;

    @MockitoBean
    private InvoiceDao invoiceDao;

    @MockitoBean
    private PartyManagementSrv.Iface partyManagementClient;
    @MockitoBean
    private RepositoryClientSrv.Iface dominantClient;

    private String partyId;
    private String shopId;
    private long expectedSum;
    private long expectedRefundSum;

    @BeforeEach
    public void setUp() throws TException {
        InvoiceRecord invoiceRecord = new InvoiceRecord();
        invoiceRecord.setProduct("vality.dev");
        invoiceRecord.setExternalId("invoice_external_id");
        invoiceRecord.setStatus(InvoiceStatus.paid);
        Mockito.when(invoiceDao.getInvoice(any())).thenReturn(invoiceRecord);
        mockDominant();
        partyId = random(String.class);
        shopId = random(String.class);
        int defaultOperationsCount = 3;

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
        expectedSum = paymentRecordList.stream()
                .mapToLong(r -> r.getOriginAmount())
                .sum();
        expectedRefundSum = refundRecordList.stream()
                .mapToLong(r -> r.getAmount())
                .sum();
    }

    @Test
    void testProcessPaymentRegistryTemplate() throws IOException, TException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("registry_of_act_", "_test_report.xlsx");

            Report report = random(Report.class);
            report.setPartyId(partyId);
            report.setPartyShopId(shopId);
            report.setFromTime(LocalDateTime.now().minusHours(128L));
            report.setToTime(LocalDateTime.now());
            report.setTimezone("Europe/Moscow");
            DomainObject testShop = getTestShop(report.getPartyShopId());
            String currencyCode = "RUB";

            given(dominantClient.checkoutObject(any(), any()))
                    .willReturn(getVersionedObject(getTestParty(partyId, shopId)))
                    .willReturn(getVersionedObject(getTesCurrency(currencyCode)));
            given(dominantClient.checkoutObjects(any(), any()))
                    .willReturn(List.of(getVersionedObject(testShop)));

            paymentRegistryTemplate.processReportTemplate(report, Files.newOutputStream(tempFile));
            Workbook wb = new XSSFWorkbook(Files.newInputStream(tempFile));
            Sheet sheet = wb.getSheetAt(0);

            String from = TimeUtil.toLocalizedDate(report.getFromTime().toInstant(ZoneOffset.UTC),
                    ZoneId.of(report.getTimezone()));
            String to = TimeUtil.toLocalizedDate(report.getToTime().toInstant(ZoneOffset.UTC),
                    ZoneId.of(report.getTimezone()));

            Cell paymentsHeaderCell = sheet.getRow(0).getCell(0);
            assertEquals(String.format("Платежи за период с %s по %s", from, to),
                    paymentsHeaderCell.getStringCellValue());

            Row paymentsFirstRow = sheet.getRow(2);
            assertEquals(testShop.getShopConfig().getData().getLocation().getUrl(),
                    paymentsFirstRow.getCell(6).getStringCellValue());
            assertEquals(FormatUtil.formatCurrency(2L, (short) 2), paymentsFirstRow.getCell(8).getStringCellValue());
            assertEquals(currencyCode, paymentsFirstRow.getCell(9).getStringCellValue());
            assertEquals("payment_external_id", paymentsFirstRow.getCell(10).getStringCellValue());
            assertEquals(captured.getLiteral(), paymentsFirstRow.getCell(11).getStringCellValue());
            assertEquals(shopId, paymentsFirstRow.getCell(12).getStringCellValue());
            assertEquals(testShop.getShopConfig().getData().getName(),
                    paymentsFirstRow.getCell(13).getStringCellValue());


            Cell paymentsTotalSum = sheet.getRow(5).getCell(3);
            assertEquals(FormatUtil.formatCurrency(expectedSum, (short) 2), paymentsTotalSum.getStringCellValue());

            Cell refundsHeaderCell = sheet.getRow(8).getCell(0);
            assertEquals(String.format("Возвраты за период с %s по %s", from, to),
                    refundsHeaderCell.getStringCellValue());

            Row refundsFirstRow = sheet.getRow(10);
            assertEquals("0", refundsFirstRow.getCell(8).getStringCellValue());
            assertEquals("You are the reason of my life", refundsFirstRow.getCell(9).getStringCellValue());
            assertEquals(currencyCode, refundsFirstRow.getCell(10).getStringCellValue());
            assertEquals(shopId, refundsFirstRow.getCell(13).getStringCellValue());
            assertEquals(testShop.getShopConfig().getData().getName(),
                    refundsFirstRow.getCell(14).getStringCellValue());
            assertEquals(testShop.getShopConfig().getData().getLocation().getUrl(),
                    refundsFirstRow.getCell(6).getStringCellValue());
            assertEquals(RefundStatus.succeeded.getLiteral(), refundsFirstRow.getCell(12).getStringCellValue());

            Cell refundsTotalSum = sheet.getRow(13).getCell(3);
            assertEquals(FormatUtil.formatCurrency(expectedRefundSum, (short) 2), refundsTotalSum.getStringCellValue());

            Cell adjustmentsHeaderCell = sheet.getRow(16).getCell(0);
            assertEquals(String.format("Корректировки за период с %s по %s", from, to),
                    adjustmentsHeaderCell.getStringCellValue());

            Row adjustmentsFirstRow = sheet.getRow(18);
            assertEquals("id0", adjustmentsFirstRow.getCell(0).getStringCellValue());
            assertEquals("1.23", adjustmentsFirstRow.getCell(3).getStringCellValue());
            assertEquals("payment_external_id", adjustmentsFirstRow.getCell(10).getStringCellValue());
            assertEquals(captured.getLiteral(), adjustmentsFirstRow.getCell(11).getStringCellValue());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @SneakyThrows
    private void mockDominant() {
        CurrencyObject currencyObject = new CurrencyObject();
        currencyObject.setRef(new CurrencyRef("RUB"))
                .setData(new Currency().setSymbolicCode("RUB")
                        .setExponent((short) 2)
                        .setName("Rubles")
                        .setNumericCode((short) 643));
        var domainObject = new DomainObject();
        domainObject.setCurrency(currencyObject);
        VersionedObject versionedObject = new VersionedObject();
        versionedObject.setObject(domainObject);
        Mockito.when(dominantClient.checkoutObject(any(), any())).thenReturn(versionedObject);

    }
}
