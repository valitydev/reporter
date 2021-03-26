package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.damsel.merch_stat.BankCard;
import com.rbkmoney.damsel.merch_stat.InvoicePaymentCaptured;
import com.rbkmoney.damsel.merch_stat.InvoicePaymentStatus;
import com.rbkmoney.damsel.merch_stat.Payer;
import com.rbkmoney.damsel.merch_stat.PaymentResourcePayer;
import com.rbkmoney.damsel.merch_stat.PaymentTool;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.config.AbstractTemplateConfig;
import com.rbkmoney.reporter.dao.ContractMetaDao;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.DaoException;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.model.StatAdjustment;
import com.rbkmoney.reporter.template.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.template.ProvisionOfServiceTemplateImpl;
import com.rbkmoney.reporter.util.BuildUtils;
import com.rbkmoney.reporter.util.FormatUtil;
import com.rbkmoney.reporter.util.TimeUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class TemplateTest extends AbstractTemplateConfig {

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private ContractMetaDao contractMetaDao;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    @Autowired
    private PaymentRegistryTemplateImpl paymentRegistryTemplate;

    @Autowired
    private ProvisionOfServiceTemplateImpl provisionOfServiceTemplate;

    @Test
    public void testProcessPaymentRegistryTemplate() throws IOException, TException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("registry_of_act_", "_test_report.xlsx");

            List<StatPayment> paymentList = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                paymentList.add(BuildUtils.buildStatPayment(i));
            }

            List<StatRefund> refundList = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                refundList.add(BuildUtils.buildStatRefund(i));
            }

            List<StatAdjustment> adjustmentList = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                adjustmentList.add(BuildUtils.buildStatAdjustment(i));
            }

            given(statisticService.getCapturedPaymentsIterator(any(), any(), any(), any()))
                    .willReturn(paymentList.iterator());

            given(statisticService.getRefundsIterator(any(), any(), any(), any()))
                    .willReturn(refundList.iterator());

            given(statisticService.getAdjustmentsIterator(any(), any(), any(), any()))
                    .willReturn(adjustmentList.iterator());

            StatPayment payment = new StatPayment();
            InvoicePaymentCaptured invoicePaymentCaptured = new InvoicePaymentCaptured();
            invoicePaymentCaptured.setAt("2018-03-22T06:12:27Z");
            payment.setStatus(InvoicePaymentStatus.captured(invoicePaymentCaptured));
            PaymentResourcePayer paymentResourcePayer =
                    new PaymentResourcePayer(PaymentTool.bank_card(new BankCard("token", null, "4249", "567890")));
            paymentResourcePayer.setEmail("xyz@mail.ru");
            payment.setPayer(Payer.payment_resource(paymentResourcePayer));

            given(statisticService.getCapturedPayment(any(), any(), any(), any()))
                    .willReturn(payment);

            Map<String, String> purposes = new HashMap<>();
            purposes.put("invoiceId0", "product0");
            purposes.put("invoiceId1", "product1");
            purposes.put("invoiceId2", "product2");

            given(statisticService.getPurposes(any(), any(), any(), any()))
                    .willReturn(purposes);

            String contractId = random(String.class);
            Report report = random(Report.class);
            report.setTimezone("Europe/Moscow");
            String registeredName = random(String.class);
            String legalSignedAt = TypeUtil.temporalToString(Instant.now());
            String legalAgreementId = random(String.class);

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
            long expectedSum = paymentList.stream().mapToLong(StatPayment::getAmount).sum();
            assertEquals(FormatUtil.formatCurrency(expectedSum), paymentsTotalSum.getStringCellValue());

            Cell refundsHeaderCell = sheet.getRow(8).getCell(0);
            assertEquals(String.format("Возвраты за период с %s по %s", from, to),
                    refundsHeaderCell.getStringCellValue());

            Row refundsFirstRow = sheet.getRow(10);
            assertEquals("0", refundsFirstRow.getCell(8).getStringCellValue());
            assertEquals("You are the reason of my life", refundsFirstRow.getCell(9).getStringCellValue());
            assertEquals("RUB", refundsFirstRow.getCell(10).getStringCellValue());

            Cell refundsTotalSum = sheet.getRow(13).getCell(3);
            long expectedRefundSum = refundList.stream().mapToLong(StatRefund::getAmount).sum();
            assertEquals(FormatUtil.formatCurrency(expectedRefundSum), refundsTotalSum.getStringCellValue());

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void generateProvisionOfServiceReportTest() throws DaoException, IOException, TException {
        Path tempFile = Files.createTempFile("provision_of_service_", "_test_report.xlsx");
        System.out.println("Provision of service report generated on " + tempFile.toAbsolutePath().toString());

        String contractId = random(String.class);
        Report report = random(Report.class);
        report.setTimezone("Europe/Moscow");
        String registeredName = random(String.class);
        String legalSignedAt = TypeUtil.temporalToString(Instant.now());
        String legalAgreementId = random(String.class);

        mockPartyManagementClient(contractId, report, registeredName, legalSignedAt, legalAgreementId);

        ShopAccountingModel previousAccounting = random(ShopAccountingModel.class);
        given(statisticService.getShopAccounting(report.getPartyId(), report.getPartyShopId(), "RUB",
                report.getFromTime().toInstant(ZoneOffset.UTC)))
                .willReturn(previousAccounting);

        ShopAccountingModel currentAccounting = random(ShopAccountingModel.class);
        given(statisticService.getShopAccounting(report.getPartyId(), report.getPartyShopId(), "RUB",
                report.getFromTime().toInstant(ZoneOffset.UTC), report.getToTime().toInstant(ZoneOffset.UTC)))
                .willReturn(currentAccounting);

        ContractMeta contractMeta = random(ContractMeta.class, "lastClosingBalance");
        contractMeta.setPartyId(report.getPartyId());
        contractMeta.setContractId(contractId);

        when(contractMetaDao.getExclusive(eq(report.getPartyId()), eq(contractId))).thenReturn(contractMeta);
        doNothing().when(contractMetaDao)
                .updateLastReportCreatedAt(eq(report.getPartyId()), eq(contractId), eq(report.getCreatedAt()));

        try {
            provisionOfServiceTemplate.processReportTemplate(report, Files.newOutputStream(tempFile));

            Workbook wb = new XSSFWorkbook(Files.newInputStream(tempFile));
            Sheet sheet = wb.getSheetAt(0);

            Row headerRow = sheet.getRow(1);
            Cell merchantContractIdCell = headerRow.getCell(0);
            assertEquals(
                    String.format("к Договору № %s от", legalAgreementId),
                    merchantContractIdCell.getStringCellValue()
            );
            Cell merchantContractSignedAtCell = headerRow.getCell(3);
            assertEquals(
                    TimeUtil.toLocalizedDate(legalSignedAt, ZoneId.of(report.getTimezone())),
                    merchantContractSignedAtCell.getStringCellValue()

            );

            Cell merchantNameCell = sheet.getRow(5).getCell(4);
            assertEquals(registeredName, merchantNameCell.getStringCellValue());

            Cell merchantIdCell = sheet.getRow(7).getCell(4);
            assertEquals(report.getPartyId(), merchantIdCell.getStringCellValue());

            Cell shopIdCell = sheet.getRow(9).getCell(4);
            assertEquals(report.getPartyShopId(), shopIdCell.getStringCellValue());

            Row dateRow = sheet.getRow(14);
            Cell fromTimeCell = dateRow.getCell(1);
            assertEquals(
                    TimeUtil.toLocalizedDate(report.getFromTime().toInstant(ZoneOffset.UTC),
                            ZoneId.of(report.getTimezone())),
                    fromTimeCell.getStringCellValue()
            );
            Cell toTimeCell = dateRow.getCell(3);
            assertEquals(
                    TimeUtil.toLocalizedDate(report.getToTime().minusNanos(1).toInstant(ZoneOffset.UTC),
                            ZoneId.of(report.getTimezone())),
                    toTimeCell.getStringCellValue()
            );

            Cell openingBalanceCell = sheet.getRow(23).getCell(3);
            assertEquals("#,##0.00", openingBalanceCell.getCellStyle().getDataFormatString());
            assertEquals(
                    FormatUtil.formatCurrency(previousAccounting.getAvailableFunds()),
                    openingBalanceCell.getStringCellValue()
            );

            Cell fundsPaidOutCell = sheet.getRow(26).getCell(3);
            assertEquals("#,##0.00", fundsPaidOutCell.getCellStyle().getDataFormatString());
            assertEquals(
                    FormatUtil.formatCurrency(currentAccounting.getFundsPaidOut()),
                    fundsPaidOutCell.getStringCellValue()
            );

            Cell fundsRefundedCell = sheet.getRow(28).getCell(3);
            assertEquals("#,##0.00", fundsRefundedCell.getCellStyle().getDataFormatString());
            assertEquals(
                    FormatUtil.formatCurrency(currentAccounting.getFundsRefunded()),
                    fundsRefundedCell.getStringCellValue()
            );

            Cell closingBalanceCell = sheet.getRow(29).getCell(3);
            assertEquals("#,##0.00", closingBalanceCell.getCellStyle().getDataFormatString());
            assertEquals(
                    FormatUtil.formatCurrency(
                            previousAccounting.getAvailableFunds() + currentAccounting.getAvailableFunds()),
                    closingBalanceCell.getStringCellValue()
            );

            Cell fundsAcquiredCell = sheet.getRow(17).getCell(3);
            assertEquals("#,##0.00", fundsAcquiredCell.getCellStyle().getDataFormatString());
            assertEquals(
                    FormatUtil.formatCurrency(currentAccounting.getFundsAcquired()),
                    fundsAcquiredCell.getStringCellValue()
            );
            assertEquals(
                    fundsAcquiredCell.getStringCellValue(),
                    sheet.getRow(24).getCell(3).getStringCellValue()
            );

            Cell feeChargedCell = sheet.getRow(19).getCell(3);
            assertEquals("#,##0.00", feeChargedCell.getCellStyle().getDataFormatString());
            assertEquals(
                    FormatUtil.formatCurrency(currentAccounting.getFeeCharged()),
                    feeChargedCell.getStringCellValue()
            );
            assertEquals(
                    feeChargedCell.getStringCellValue(),
                    sheet.getRow(25).getCell(3).getStringCellValue()
            );

            assertEquals(
                    contractMeta.getRepresentativePosition(),
                    sheet.getRow(40).getCell(4).getStringCellValue()
            );
            assertEquals(
                    contractMeta.getRepresentativeFullName() + ",",
                    sheet.getRow(41).getCell(4).getStringCellValue()
            );

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void mockPartyManagementClient(String contractId, Report report, String registeredName,
                                           String legalSignedAt, String legalAgreementId) throws TException {
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
