package com.rbkmoney.reporter.report;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.config.AbstractLocalTemplateConfig;
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
import com.rbkmoney.reporter.template.LocalPaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.util.FormatUtil;
import com.rbkmoney.reporter.util.TimeUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
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
import java.util.*;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class PaymentRegistryTemplateTest extends AbstractLocalTemplateConfig {

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

    @Before
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
            assertEquals(FormatUtil.formatCurrency(2L), paymentsFirstRow.getCell(8).getStringCellValue());
            assertEquals("RUB", paymentsFirstRow.getCell(9).getStringCellValue());

            Cell paymentsTotalSum = sheet.getRow(5).getCell(3);
            assertEquals(FormatUtil.formatCurrency(expectedSum), paymentsTotalSum.getStringCellValue());

            Cell refundsHeaderCell = sheet.getRow(8).getCell(0);
            assertEquals(String.format("Возвраты за период с %s по %s", from, to),
                    refundsHeaderCell.getStringCellValue());

            Row refundsFirstRow = sheet.getRow(10);
            assertEquals("0", refundsFirstRow.getCell(8).getStringCellValue());
            assertEquals("You are the reason of my life", refundsFirstRow.getCell(9).getStringCellValue());
            assertEquals("RUB", refundsFirstRow.getCell(10).getStringCellValue());

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
        shop.setId(report.getPartyId());
        shop.setContractId(contractId);
        shop.setLocation(ShopLocation.url("http://0ch.ru/b"));
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

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(party);
        given(partyManagementClient.getRevision(any(), any()))
                .willReturn(1L);
    }
}
