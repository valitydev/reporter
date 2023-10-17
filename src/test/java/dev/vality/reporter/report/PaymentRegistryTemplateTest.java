package dev.vality.reporter.report;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.reporter.config.PostgresqlSpringBootITest;
import dev.vality.reporter.dao.AdjustmentDao;
import dev.vality.reporter.dao.InvoiceDao;
import dev.vality.reporter.dao.PaymentDao;
import dev.vality.reporter.dao.RefundDao;
import dev.vality.reporter.data.ReportsTestData;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.vality.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@PostgresqlSpringBootITest
public class PaymentRegistryTemplateTest {

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @Autowired
    private LocalPaymentRegistryTemplateImpl paymentRegistryTemplate;

    @MockBean
    private InvoiceDao invoiceDao;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    private String partyId;
    private String shopId;
    private long expectedSum;
    private long expectedRefundSum;

    @BeforeEach
    public void setUp() {
        InvoiceRecord invoiceRecord = new InvoiceRecord();
        invoiceRecord.setProduct("rbk.money");
        Mockito.when(invoiceDao.getInvoice(any())).thenReturn(invoiceRecord);
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
                .mapToLong(r -> r.getAmount())
                .sum();
        expectedRefundSum = refundRecordList.stream()
                .mapToLong(r -> r.getAmount())
                .sum();
    }

    @Test
    public void testProcessPaymentRegistryTemplate() throws IOException, TException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("registry_of_act_", "_test_report.xlsx");

            Report report = random(Report.class);
            report.setPartyId(partyId);
            report.setPartyShopId(shopId);
            report.setFromTime(LocalDateTime.now().minusHours(128L));
            report.setToTime(LocalDateTime.now());
            report.setTimezone("Europe/Moscow");
            String registeredName = random(String.class);
            String legalSignedAt = TypeUtil.temporalToString(Instant.now());
            String legalAgreementId = random(String.class);
            String contractId = random(String.class);

            mockPartyManagementClient(contractId, report, registeredName, legalSignedAt, legalAgreementId);

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
            Assertions.assertEquals(FormatUtil.formatCurrency(2L), paymentsFirstRow.getCell(8).getStringCellValue());
            assertEquals("RUB", paymentsFirstRow.getCell(9).getStringCellValue());
            assertEquals(shopId, paymentsFirstRow.getCell(12).getStringCellValue());
            assertEquals("Test shop", paymentsFirstRow.getCell(13).getStringCellValue());
            assertEquals(dev.vality.reporter.domain.enums.InvoicePaymentStatus.captured.getLiteral(),
                    paymentsFirstRow.getCell(11).getStringCellValue());
            assertEquals("http://0ch.ru/b", paymentsFirstRow.getCell(6).getStringCellValue());


            Cell paymentsTotalSum = sheet.getRow(5).getCell(3);
            assertEquals(FormatUtil.formatCurrency(expectedSum), paymentsTotalSum.getStringCellValue());

            Cell refundsHeaderCell = sheet.getRow(8).getCell(0);
            assertEquals(String.format("Возвраты за период с %s по %s", from, to),
                    refundsHeaderCell.getStringCellValue());

            Row refundsFirstRow = sheet.getRow(10);
            assertEquals("0", refundsFirstRow.getCell(8).getStringCellValue());
            assertEquals("You are the reason of my life", refundsFirstRow.getCell(9).getStringCellValue());
            assertEquals("RUB", refundsFirstRow.getCell(10).getStringCellValue());
            assertEquals(shopId, refundsFirstRow.getCell(13).getStringCellValue());
            assertEquals("Test shop", refundsFirstRow.getCell(14).getStringCellValue());
            assertEquals("http://0ch.ru/b", refundsFirstRow.getCell(6).getStringCellValue());
            assertEquals(RefundStatus.succeeded.getLiteral(), refundsFirstRow.getCell(12).getStringCellValue());

            Cell refundsTotalSum = sheet.getRow(13).getCell(3);
            assertEquals(FormatUtil.formatCurrency(expectedRefundSum), refundsTotalSum.getStringCellValue());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void mockPartyManagementClient(String contractId,
                                           Report report,
                                           String registeredName,
                                           String legalSignedAt,
                                           String legalAgreementId) throws TException {
        Shop shop = new Shop();
        shop.setId(report.getPartyShopId());
        shop.setContractId(contractId);
        shop.setLocation(ShopLocation.url("http://0ch.ru/b"));
        shop.setDetails(new ShopDetails().setName("Test shop"));
        RussianLegalEntity russianLegalEntity = new RussianLegalEntity();
        russianLegalEntity.setRegisteredName(registeredName);
        russianLegalEntity.setRepresentativePosition(random(String.class));
        russianLegalEntity.setRepresentativeFullName(random(String.class));
        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setContractor(Contractor.legal_entity(LegalEntity.russian_legal_entity(russianLegalEntity)));
        contract.setLegalAgreement(new LegalAgreement(legalSignedAt, legalAgreementId));
        Party party = new Party();
        party.setId(report.getPartyId());
        party.setShops(Collections.singletonMap(report.getPartyShopId(), shop));
        party.setContracts(Collections.singletonMap(contractId, contract));

        given(partyManagementClient.checkout(any(), any()))
                .willReturn(party);
        given(partyManagementClient.getRevision(any()))
                .willReturn(1L);
    }
}
