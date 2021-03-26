package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.config.AbstractLocalTemplateConfig;
import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.reporter.domain.tables.records.InvoiceRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.rbkmoney.reporter.model.StatAdjustment;
import com.rbkmoney.reporter.template.LocalPaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.template.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.util.BuildUtils;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class LocalTemplateTest extends AbstractLocalTemplateConfig {

    @Autowired
    private PaymentRegistryTemplateImpl paymentRegistryTemplate;

    @Autowired
    private LocalPaymentRegistryTemplateImpl localPaymentRegistryTemplate;

    @Autowired
    private LocalStatisticService localStatisticService;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @MockBean
    private InvoiceDao invoiceDao;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private ContractMetaDao contractMetaDao;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    private String partyId;
    private String shopId;

    @Before
    public void setUp() {
        InvoiceRecord invoiceRecord = new InvoiceRecord();
        invoiceRecord.setProduct("rbk.money");
        Mockito.when(invoiceDao.getInvoice(any())).thenReturn(invoiceRecord);
        partyId = random(String.class);
        shopId = random(String.class);
        List<StatPayment> statPaymentList = new ArrayList<>();
        List<PaymentRecord> paymentRecordList = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            statPaymentList.add(BuildUtils.buildStatPayment(i, shopId));
            PaymentRecord paymentRecord = BuildUtils.buildPaymentRecord(i, partyId, shopId);
            paymentRecordList.add(paymentRecord);
            paymentDao.savePayment(paymentRecord.into(Payment.class));
        }

        List<StatRefund> statRefundList = new ArrayList<>();
        List<RefundRecord> refundRecordList = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            statRefundList.add(BuildUtils.buildStatRefund(i, shopId));
            RefundRecord refundRecord = BuildUtils.buildRefundRecord(i, partyId, shopId);
            refundRecordList.add(refundRecord);
            refundDao.saveRefund(refundRecord.into(Refund.class));
        }

        List<StatAdjustment> adjustmentList = new ArrayList<>();
        List<AdjustmentRecord> adjustmentRecordList = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            adjustmentList.add(BuildUtils.buildStatAdjustment(i, shopId));
            AdjustmentRecord adjustmentRecord = BuildUtils.buildStatAdjustmentRecord(i, partyId, shopId);
            adjustmentRecordList.add(adjustmentRecord);
            adjustmentDao.saveAdjustment(adjustmentRecord.into(Adjustment.class));
        }

        given(statisticService.getCapturedPaymentsIterator(any(), any(), any(), any()))
                .willReturn(statPaymentList.iterator());
        given(statisticService.getRefundsIterator(any(), any(), any(), any()))
                .willReturn(statRefundList.iterator());
        given(statisticService.getAdjustmentsIterator(any(), any(), any(), any()))
                .willReturn(adjustmentList.iterator());
        given(statisticService.getCapturedPayment(any(), any(), any(), any()))
                .willReturn(BuildUtils.buildStatPayment());
        given(statisticService.getPurposes(any(), any(), any(), any()))
                .willReturn(BuildUtils.buildPurposes(3));
    }

    @Test
    public void comparePaymentRegistryTemplateTest() throws IOException, TException {
        Path magistaReport = null;
        Path localReport = null;
        try {
            magistaReport = Files.createTempFile("registry_of_act_", "_test_report.xlsx");
            localReport = Files.createTempFile("local_registry_of_act_", "_test_report.xlsx");

            Report report = random(Report.class);
            report.setFromTime(LocalDateTime.now().minus(110L, ChronoUnit.YEARS));
            report.setToTime(LocalDateTime.now());
            report.setTimezone("Europe/Moscow");
            report.setPartyId(partyId);
            report.setPartyShopId(shopId);
            String registeredName = random(String.class);
            String legalSignedAt = TypeUtil.temporalToString(Instant.now());
            String legalAgreementId = random(String.class);

            String contractId = random(String.class);
            mockPartyManagementClient(contractId, report, registeredName, legalSignedAt, legalAgreementId);
            paymentRegistryTemplate.processReportTemplate(report, Files.newOutputStream(magistaReport));
            localPaymentRegistryTemplate.processReportTemplate(report, Files.newOutputStream(localReport));
            Workbook magistaWb = new XSSFWorkbook(Files.newInputStream(magistaReport));
            Sheet magistaSheet = magistaWb.getSheetAt(0);
            Workbook localWb = new XSSFWorkbook(Files.newInputStream(localReport));
            Sheet localSheet = localWb.getSheetAt(0);

            Cell magistaPaymentsHeaderCell = magistaSheet.getRow(0).getCell(0);
            Cell localPaymentsHeaderCell = localSheet.getRow(0).getCell(0);
            assertEquals(magistaPaymentsHeaderCell.getStringCellValue(), localPaymentsHeaderCell.getStringCellValue());

            Row magistaPaymentsFirstRow = magistaSheet.getRow(2);
            Row localPaymentsFirstRow = localSheet.getRow(2);
            assertEquals(magistaPaymentsFirstRow.getCell(8).getStringCellValue(),
                    localPaymentsFirstRow.getCell(8).getStringCellValue());
            assertEquals(magistaPaymentsFirstRow.getCell(9).getStringCellValue(),
                    localPaymentsFirstRow.getCell(9).getStringCellValue());

            Cell magistaPaymentsTotalSum = magistaSheet.getRow(5).getCell(3);
            Cell localPaymentsTotalSum = localSheet.getRow(5).getCell(3);
            assertEquals(magistaPaymentsTotalSum.getStringCellValue(), localPaymentsTotalSum.getStringCellValue());

            Cell magistaRefundsHeaderCell = magistaSheet.getRow(8).getCell(0);
            Cell localRefundsHeaderCell = localSheet.getRow(8).getCell(0);
            assertEquals(magistaRefundsHeaderCell.getStringCellValue(), localRefundsHeaderCell.getStringCellValue());

            Row magistaRefundsFirstRow = magistaSheet.getRow(10);
            Row localRefundsFirstRow = localSheet.getRow(10);
            assertEquals(magistaRefundsFirstRow.getCell(8).toString(), localRefundsFirstRow.getCell(8).toString());
            assertEquals(magistaRefundsFirstRow.getCell(9).getStringCellValue(),
                    localRefundsFirstRow.getCell(9).getStringCellValue());
            assertEquals(magistaRefundsFirstRow.getCell(10).getStringCellValue(),
                    localRefundsFirstRow.getCell(10).getStringCellValue());

            Cell magistaRefundsTotalSum = magistaSheet.getRow(13).getCell(3);
            Cell localRefundsTotalSum = localSheet.getRow(13).getCell(3);
            assertEquals(magistaRefundsTotalSum.getStringCellValue(), localRefundsTotalSum.getStringCellValue());
        } finally {
            Files.deleteIfExists(magistaReport);
            Files.deleteIfExists(localReport);
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
