package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.service.impl.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.util.FormatUtil;
import com.rbkmoney.reporter.util.TimeUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.thrift.TException;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

/**
 * Created by tolkonepiu on 12/07/2017.
 */
public class PaymentRegistryTemplateServiceTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentRegistryTemplateImpl templateService;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private PartyService partyService;

    @Test
    public void testProcessPaymentRegistryTemplate() throws IOException, TException {
        Path tempFile = Files.createTempFile("registry_of_act_", "_test_report.xlsx");
        System.out.println("Registry of act report generated on " + tempFile.toAbsolutePath().toString());

        List<StatPayment> paymentList = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            StatPayment payment = new StatPayment();
            payment.setId("id" + i);
            payment.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
            payment.setInvoiceId("invoiceId" + i);
            InvoicePaymentStatus invoicePaymentStatus;
            if (i < 2) {
                InvoicePaymentCaptured invoicePaymentCaptured = new InvoicePaymentCaptured();
                invoicePaymentCaptured.setAt("201" + (3 - i) + "-03-22T06:12:27Z");
                invoicePaymentStatus = InvoicePaymentStatus.captured(invoicePaymentCaptured);
            } else {
                InvoicePaymentRefunded invoicePaymentRefunded = new InvoicePaymentRefunded();
                invoicePaymentRefunded.setAt("201" + (3 - i) + "-03-22T06:12:27Z");
                invoicePaymentStatus = InvoicePaymentStatus.refunded(invoicePaymentRefunded);
            }
            payment.setStatus(invoicePaymentStatus);

            PaymentResourcePayer paymentResourcePayer = new PaymentResourcePayer(PaymentTool.bank_card(new BankCard("token", null, "424" + i, "56789" + i)), "sessionId");
            paymentResourcePayer.setEmail("abc" + i + "@mail.ru");
            payment.setPayer(Payer.payment_resource(paymentResourcePayer));
            payment.setAmount(123L + i);
            payment.setFee(2L + i);
            payment.setShopId("shopId" + i);
            paymentList.add(payment);
        }

        List<StatRefund> refundList = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            StatRefund refund = new StatRefund();
            refund.setId("id" + i);
            refund.setPaymentId("paymentId" + i);
            refund.setInvoiceId("invoiceId" + i);
            refund.setStatus(InvoicePaymentRefundStatus.succeeded(new InvoicePaymentRefundSucceeded("201" + i + "-03-22T06:12:27Z")));
            refund.setAmount(123L + i);
            refund.setShopId("shopId" + i);
            refundList.add(refund);
        }

        given(statisticService.getPayments(any(), any(), any(), any(), any()))
                .willAnswer((Answer<List<StatPayment>>) invocationOnMock -> {
                    InvoicePaymentStatus status = invocationOnMock.getArgumentAt(4, InvoicePaymentStatus.class);
                    if (status.isSetCaptured()) {
                        return paymentList.stream().filter(p -> p.getStatus().isSetCaptured()).collect(Collectors.toList());
                    } else if (status.isSetRefunded()) {
                        return paymentList.stream().filter(p -> p.getStatus().isSetRefunded()).collect(Collectors.toList());
                    }
                    return null;
                });

        given(statisticService.getRefunds(any(), any(), any(), any(), any()))
                .willReturn(refundList);

        StatPayment payment = new StatPayment();
        InvoicePaymentCaptured invoicePaymentCaptured = new InvoicePaymentCaptured();
        invoicePaymentCaptured.setAt("2018-03-22T06:12:27Z");
        payment.setStatus(InvoicePaymentStatus.captured(invoicePaymentCaptured));
        PaymentResourcePayer paymentResourcePayer = new PaymentResourcePayer(PaymentTool.bank_card(new BankCard("token", null, "4249", "567890")), "sessionId");
        paymentResourcePayer.setEmail("xyz@mail.ru");
        payment.setPayer(Payer.payment_resource(paymentResourcePayer));

        given(statisticService.getPayment(any(), any()))
                .willReturn(payment);

        List<StatInvoice> invoices = new ArrayList<>();
        StatInvoice i = new StatInvoice();
        i.setId("invoiceId0");
        i.setProduct("product0");
        invoices.add(i);
        i = new StatInvoice();
        i.setId("invoiceId1");
        i.setProduct("product1");
        invoices.add(i);
        i = new StatInvoice();
        i.setId("invoiceId2");
        i.setProduct("product2");
        invoices.add(i);
        given(statisticService.getInvoices(any(), any(), any(), any()))
                .willReturn(invoices);

        Map<String, String> shops = new HashMap<>();
        shops.put("shopId0", "http://0ch.ru/b");
        shops.put("shopId1", "http://1ch.ru/b");
        shops.put("shopId2", "http://2ch.ru/b");

        given(partyService.getShopUrls(any(), any(), any()))
                .willReturn(shops);

        Report report = new Report(random(Long.class), LocalDateTime.now().minusMonths(1), LocalDateTime.now().plusDays(1), random(LocalDateTime.class), random(String.class), random(String.class), random(ReportStatus.class), "Europe/Moscow", random(ReportType.class));
        ContractMeta contractMeta = random(ContractMeta.class);

        try {
            templateService.processReportTemplate(report, contractMeta, Files.newOutputStream(tempFile));
            Workbook wb = new XSSFWorkbook(Files.newInputStream(tempFile));
            Sheet sheet = wb.getSheetAt(0);


            String from = TimeUtil.toLocalizedDate(report.getFromTime().toInstant(ZoneOffset.UTC), ZoneId.of(report.getTimezone()));
            String to = TimeUtil.toLocalizedDate(report.getToTime().toInstant(ZoneOffset.UTC), ZoneId.of(report.getTimezone()));

            Cell paymentsHeaderCell = sheet.getRow(0).getCell(0);
            assertEquals(String.format("Платежи за период с %s по %s", from, to), paymentsHeaderCell.getStringCellValue());

            Cell paymentsTotalSum = sheet.getRow(5).getCell(3);
            long expectedSum = paymentList.stream().mapToLong(StatPayment::getAmount).sum();
            assertTrue(FormatUtil.formatCurrency(expectedSum) - paymentsTotalSum.getNumericCellValue() < 0.00001);

            Cell refundsHeaderCell = sheet.getRow(8).getCell(0);
            assertEquals(String.format("Возвраты за период с %s по %s", from, to), refundsHeaderCell.getStringCellValue());

            Cell refundsTotalSum = sheet.getRow(13).getCell(3);
            long expectedRefundSum = refundList.stream().mapToLong(StatRefund::getAmount).sum();
            assertTrue(FormatUtil.formatCurrency(expectedRefundSum) - refundsTotalSum.getNumericCellValue() < 0.00001);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}
