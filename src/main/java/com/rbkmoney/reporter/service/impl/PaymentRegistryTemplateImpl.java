package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.StatisticService;
import com.rbkmoney.reporter.service.TemplateService;
import com.rbkmoney.reporter.util.FormatUtil;
import com.rbkmoney.reporter.util.TimeUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PaymentRegistryTemplateImpl implements TemplateService {

    private final StatisticService statisticService;

    private final PartyService partyService;

    @Autowired
    public PaymentRegistryTemplateImpl(
            StatisticService statisticService,
            PartyService partyService) {
        this.statisticService = statisticService;
        this.partyService = partyService;
    }

    @Override
    public boolean accept(ReportType reportType, ContractMeta contractMeta) {
        return reportType == ReportType.payment_registry
                || (contractMeta.getReportType() == ReportType.provision_of_service
                && partyService.needReference(contractMeta.getPartyId(), contractMeta.getContractId()));
    }

    @Override
    public void processReportTemplate(Report report, ContractMeta contractMeta, OutputStream outputStream) throws
            IOException {
        ZoneId reportZoneId = ZoneId.of(report.getTimezone());
        String fromTime = TimeUtil.toLocalizedDate(report.getFromTime().toInstant(ZoneOffset.UTC), reportZoneId);
        String toTime = TimeUtil.toLocalizedDate(report.getToTime().minusNanos(1).toInstant(ZoneOffset.UTC), reportZoneId);

        Map<String, String> shopUrls = partyService.getShopUrls(report.getPartyId(), report.getPartyContractId(), report.getCreatedAt().toInstant(ZoneOffset.UTC));

        Map<String, String> purposes = statisticService.getPurposes(
                report.getPartyId(),
                report.getPartyContractId(),
                report.getFromTime().toInstant(ZoneOffset.UTC),
                report.getToTime().toInstant(ZoneOffset.UTC)
        );

        AtomicLong totalAmnt = new AtomicLong();
        AtomicLong totalPayoutAmnt = new AtomicLong();
        Iterator<StatPayment> paymentsIterator = statisticService.getCapturedPaymentsIterator(
                report.getPartyId(),
                report.getPartyContractId(),
                report.getFromTime().toInstant(ZoneOffset.UTC),
                report.getToTime().toInstant(ZoneOffset.UTC)
        );

        SXSSFWorkbook wb = new SXSSFWorkbook(100); // keep 100 rows in memory, exceeding rows will be flushed to disk
        Sheet sh = wb.createSheet();
        sh.setDefaultColumnWidth(20);
        int rownum = 0;
        Row rowFirstPayments = sh.createRow(rownum++);

        for (int i = 0; i < 8; ++i) {
            rowFirstPayments.createCell(i);
        }
        sh.addMergedRegion(new CellRangeAddress(rownum - 1, rownum - 1, 0, 7));
        Cell cellFirstPayments = rowFirstPayments.getCell(0);
        cellFirstPayments.setCellValue(String.format("Платежи за период с %s по %s", fromTime, toTime));
        CellUtil.setAlignment(cellFirstPayments, HorizontalAlignment.CENTER);
        Font font = wb.createFont();
        font.setBold(true);
        CellUtil.setFont(cellFirstPayments, font);

        Row rowSecondPayments = sh.createRow(rownum++);
        CellStyle greyStyle = wb.createCellStyle();
        greyStyle.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        greyStyle.setFillPattern(FillPatternType.LESS_DOTS);
        for (int i = 0; i < 8; ++i) {
            Cell cell = rowSecondPayments.createCell(i);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            cell.setCellStyle(greyStyle);
            CellUtil.setFont(cell, font);
        }
        rowSecondPayments.getCell(0).setCellValue("Дата");
        rowSecondPayments.getCell(1).setCellValue("Id платежа");
        rowSecondPayments.getCell(2).setCellValue("Метод оплаты");
        rowSecondPayments.getCell(3).setCellValue("Сумма платежа");
        rowSecondPayments.getCell(4).setCellValue("Сумма к выводу");
        rowSecondPayments.getCell(5).setCellValue("Email плательщика");
        rowSecondPayments.getCell(6).setCellValue("URL магазина");
        rowSecondPayments.getCell(7).setCellValue("Назначение платежа");

        while (paymentsIterator.hasNext()) {
            StatPayment p = paymentsIterator.next();
            Row row = sh.createRow(rownum++);
            row.createCell(0).setCellValue(p.getInvoiceId() + "." + p.getId());
            row.createCell(1).setCellValue(TimeUtil.toLocalizedDateTime(p.getStatus().getCaptured().getAt(), reportZoneId));
            String paymentTool = null;
            if (p.getPayer().isSetPaymentResource()) {
                paymentTool = p.getPayer().getPaymentResource().getPaymentTool().getSetField().getFieldName();
            }
            row.createCell(2).setCellValue(paymentTool);
            row.createCell(3).setCellValue(FormatUtil.formatCurrency(p.getAmount()));
            row.createCell(4).setCellValue(FormatUtil.formatCurrency(p.getAmount() - p.getFee()));
            totalAmnt.addAndGet(p.getAmount());
            totalPayoutAmnt.addAndGet(p.getAmount() - p.getFee());
            String payerEmail = null;
            if (p.getPayer().isSetPaymentResource()) {
                payerEmail = p.getPayer().getPaymentResource().getEmail();
            }
            row.createCell(5).setCellValue(payerEmail);
            row.createCell(6).setCellValue(shopUrls.get(p.getShopId()));
            String purpose = purposes.get(p.getInvoiceId());
            if (purpose == null) {
                StatInvoice invoice = statisticService.getInvoice(p.getInvoiceId());
                purpose = invoice.getProduct();
            }
            row.createCell(7).setCellValue(purpose);
        }

        CellStyle borderStyle = wb.createCellStyle();
        borderStyle.setBorderBottom(BorderStyle.MEDIUM);
        borderStyle.setBorderTop(BorderStyle.MEDIUM);
        borderStyle.setBorderRight(BorderStyle.MEDIUM);
        borderStyle.setBorderLeft(BorderStyle.MEDIUM);

        //---- total amount ---------
        Row rowTotalPaymentAmount = sh.createRow(rownum++);
        for (int i = 0; i < 5; ++i) {
            Cell cell = rowTotalPaymentAmount.createCell(i);
            cell.setCellStyle(borderStyle);
            CellUtil.setFont(cell, font);
        }
        sh.addMergedRegion(new CellRangeAddress(rownum - 1, rownum - 1, 0, 2));
        Cell cellTotalPaymentAmount = rowTotalPaymentAmount.getCell(0);
        cellTotalPaymentAmount.setCellValue("Сумма");
        CellUtil.setAlignment(cellTotalPaymentAmount, HorizontalAlignment.CENTER);
        rowTotalPaymentAmount.getCell(3).setCellValue(FormatUtil.formatCurrency(totalAmnt.longValue()));
        rowTotalPaymentAmount.getCell(4).setCellValue(FormatUtil.formatCurrency(totalPayoutAmnt.longValue()));

        //-----skip rows -------
        sh.createRow(rownum++);
        sh.createRow(rownum++);

        Row rowFirstRefunds = sh.createRow(rownum++);
        for (int i = 0; i < 8; ++i) {
            rowFirstRefunds.createCell(i);
        }
        sh.addMergedRegion(new CellRangeAddress(rownum - 1, rownum - 1, 0, 7));
        Cell cellFirstRefunds = rowFirstRefunds.getCell(0);
        cellFirstRefunds.setCellValue(String.format("Возвраты за период с %s по %s", fromTime, toTime));
        CellUtil.setAlignment(cellFirstRefunds, HorizontalAlignment.CENTER);
        CellUtil.setFont(cellFirstRefunds, font);
        Row rowSecondRefunds = sh.createRow(rownum++);
        for (int i = 0; i < 8; ++i) {
            Cell cell = rowSecondRefunds.createCell(i);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            cell.setCellStyle(greyStyle);
            CellUtil.setFont(cell, font);
        }
        rowSecondRefunds.getCell(0).setCellValue("Дата возврата");
        rowSecondRefunds.getCell(1).setCellValue("Дата платежа");
        rowSecondRefunds.getCell(2).setCellValue("Id платежа");
        rowSecondRefunds.getCell(3).setCellValue("Сумма возврата");
        rowSecondRefunds.getCell(4).setCellValue("Метод оплаты");
        rowSecondRefunds.getCell(5).setCellValue("Email плательщика");
        rowSecondRefunds.getCell(6).setCellValue("URL магазина");
        rowSecondRefunds.getCell(7).setCellValue("Назначение платежа");

        AtomicLong totalRefundAmnt = new AtomicLong();
        Iterator<StatRefund> refundsIterator = statisticService.getRefundsIterator(
                report.getPartyId(),
                report.getPartyContractId(),
                report.getFromTime().toInstant(ZoneOffset.UTC),
                report.getToTime().toInstant(ZoneOffset.UTC),
                InvoicePaymentRefundStatus.succeeded(new InvoicePaymentRefundSucceeded())
        );
        while (refundsIterator.hasNext()) {
            StatRefund r = refundsIterator.next();
            Row row = sh.createRow(rownum++);
            StatPayment statPayment = statisticService.getPayment(report.getPartyId(), report.getPartyContractId(), r.getInvoiceId(), r.getPaymentId());
            row.createCell(0).setCellValue(TimeUtil.toLocalizedDateTime(r.getStatus().getSucceeded().getAt(), reportZoneId));
            //TODO captured_at only
            row.createCell(1).setCellValue(statPayment.getStatus().isSetCaptured() ?
                    TimeUtil.toLocalizedDateTime(statPayment.getStatus().getCaptured().getAt(), reportZoneId) :
                    TimeUtil.toLocalizedDateTime(statPayment.getCreatedAt(), reportZoneId));
            row.createCell(2).setCellValue(r.getInvoiceId() + "." + r.getPaymentId());
            row.createCell(3).setCellValue(FormatUtil.formatCurrency(r.getAmount()));
            String paymentTool = null;
            if (statPayment.getPayer().isSetPaymentResource()) {
                paymentTool = statPayment.getPayer().getPaymentResource().getPaymentTool().getSetField().getFieldName();
            }
            row.createCell(4).setCellValue(paymentTool);
            totalRefundAmnt.addAndGet(r.getAmount());
            String payerEmail = null;
            if (statPayment.getPayer().isSetPaymentResource()) {
                payerEmail = statPayment.getPayer().getPaymentResource().getEmail();
            }
            row.createCell(5).setCellValue(payerEmail);
            row.createCell(6).setCellValue(shopUrls.get(r.getShopId()));
            String purpose = purposes.get(r.getInvoiceId());
            if (purpose == null) {
                StatInvoice invoice = statisticService.getInvoice(r.getInvoiceId());
                purpose = invoice.getProduct();
            }
            row.createCell(7).setCellValue(purpose);
        }

        //---- total refund amount ---------
        Row rowTotalRefundAmount = sh.createRow(rownum++);
        for (int i = 0; i < 4; ++i) {
            Cell cell = rowTotalRefundAmount.createCell(i);
            cell.setCellStyle(borderStyle);
            CellUtil.setFont(cell, font);
        }
        sh.addMergedRegion(new CellRangeAddress(rownum - 1, rownum - 1, 0, 2));
        Cell cellTotalRefundAmount = rowTotalRefundAmount.getCell(0);
        cellTotalRefundAmount.setCellValue("Сумма");
        CellUtil.setAlignment(cellTotalRefundAmount, HorizontalAlignment.CENTER);
        rowTotalRefundAmount.getCell(3).setCellValue(FormatUtil.formatCurrency(totalRefundAmnt.longValue()));

        wb.write(outputStream);
        outputStream.close();
        wb.dispose();
    }

}
