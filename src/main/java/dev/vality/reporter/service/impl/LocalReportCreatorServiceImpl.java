package dev.vality.reporter.service.impl;

import dev.vality.reporter.domain.enums.PaymentPayerType;
import dev.vality.reporter.domain.tables.records.AdjustmentRecord;
import dev.vality.reporter.domain.tables.records.PaymentRecord;
import dev.vality.reporter.domain.tables.records.RefundRecord;
import dev.vality.reporter.model.Cash;
import dev.vality.reporter.model.LocalReportCreatorDto;
import dev.vality.reporter.service.DominantService;
import dev.vality.reporter.service.LocalStatisticService;
import dev.vality.reporter.service.ReportCreatorService;
import dev.vality.reporter.util.FormatUtil;
import dev.vality.reporter.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jooq.Cursor;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Setter
@Service
@RequiredArgsConstructor
public class LocalReportCreatorServiceImpl implements ReportCreatorService<LocalReportCreatorDto> {

    private final DominantService dominantService;
    private final LocalStatisticService localStatisticService;

    @Value("${report.includeAdjustments}")
    private boolean includeAdjustments;

    private int limit = SpreadsheetVersion.EXCEL2007.getLastRowIndex();

    private static final int PACKAGE_SIZE = 25;
    private static final int PAYMENTS_HEAD_ROW = 13;
    private static final int ADJUSTMENT_HEAD_ROW = 9;
    private static final int REFUNDS_HEAD_ROW = 14;
    private static final int PAYMENTS_COLUMNS_DESCRIPTION_ROW = 14;
    private static final int ADJUSTMENT_COLUMNS_DESCRIPTION_ROW = 9;
    private static final int REFUNDS_COLUMNS_DESCRIPTION_ROW = 15;

    @Override
    public void createReport(LocalReportCreatorDto reportCreatorDto) throws IOException {
        log.info("Start create report {}", reportCreatorDto.getReport().getId());
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            wb.setCompressTempFiles(true);
            Sheet sh = createSheet(wb);
            AtomicInteger rownum = new AtomicInteger(0);

            sh = createPaymentTable(reportCreatorDto, wb, sh, rownum);
            sh = addIndent(wb, sh, rownum);
            sh = createRefundTable(reportCreatorDto, wb, sh, rownum);

            if (includeAdjustments) {
                sh = addIndent(wb, sh, rownum);
                createAdjustmentTable(reportCreatorDto, wb, sh, rownum);
            }

            wb.write(reportCreatorDto.getOutputStream());
            reportCreatorDto.getOutputStream().close();
            wb.dispose();
            log.info("Report {} were created", reportCreatorDto.getReport().getId());
        }
    }

    private Sheet createPaymentTable(LocalReportCreatorDto reportCreatorDto,
                                     SXSSFWorkbook wb,
                                     Sheet sh,
                                     AtomicInteger rownum) {
        createPaymentsHeadRow(reportCreatorDto, wb, sh, rownum);
        createPaymentsColumnsDescriptionRow(wb, sh, rownum);

        Cash totalAmnt = new Cash();

        Cursor<PaymentRecord> paymentsCursor = reportCreatorDto.getPaymentsCursor();
        while (paymentsCursor.hasNext()) {
            Result<PaymentRecord> paymentRecords = paymentsCursor.fetchNext(PACKAGE_SIZE);
            for (PaymentRecord paymentRecord : paymentRecords) {

                createPaymentRow(reportCreatorDto, sh, totalAmnt, rownum, paymentRecord);
                sh = checkAndReset(wb, sh, rownum);
            }
        }
        sh = checkAndReset(wb, sh, rownum);
        createTotalAmountRow(wb, sh, totalAmnt, rownum);
        return checkAndReset(wb, sh, rownum);
    }

    private Sheet createRefundTable(LocalReportCreatorDto reportCreatorDto,
                                    SXSSFWorkbook wb,
                                    Sheet sh,
                                    AtomicInteger rownum) {
        createRefundsHeadRow(reportCreatorDto, wb, sh, rownum);
        sh = checkAndReset(wb, sh, rownum);
        createRefundsColumnsDescriptionRow(wb, sh, rownum);
        sh = checkAndReset(wb, sh, rownum);

        Cash totalRefundAmnt = new Cash();
        Cursor<RefundRecord> refundsCursor = reportCreatorDto.getRefundsCursor();
        while (refundsCursor.hasNext()) {
            Result<RefundRecord> refundRecords = refundsCursor.fetchNext(PACKAGE_SIZE);
            for (RefundRecord refundRecord : refundRecords) {
                createRefundRow(reportCreatorDto, sh, totalRefundAmnt, rownum, refundRecord);
                sh = checkAndReset(wb, sh, rownum);
            }

        }
        sh = checkAndReset(wb, sh, rownum);
        createTotalRefundAmountRow(wb, sh, totalRefundAmnt, rownum);
        return checkAndReset(wb, sh, rownum);
    }

    private Sheet createAdjustmentTable(LocalReportCreatorDto reportCreatorDto,
                                        SXSSFWorkbook wb,
                                        Sheet sh,
                                        AtomicInteger rownum) {
        createAdjustmentsHeadRow(reportCreatorDto, wb, sh, rownum);
        sh = checkAndReset(wb, sh, rownum);
        createAdjustmentColumnsDescriptionRow(wb, sh, rownum);
        sh = checkAndReset(wb, sh, rownum);

        Cash totalAdjustmentAmnt = new Cash();
        Cursor<AdjustmentRecord> adjustmentsCursor = reportCreatorDto.getAdjustmentsCursor();
        while (adjustmentsCursor.hasNext()) {
            Result<AdjustmentRecord> adjustmentRecords = adjustmentsCursor.fetchNext(PACKAGE_SIZE);
            for (AdjustmentRecord adjustmentRecord : adjustmentRecords) {
                createAdjustmentRow(reportCreatorDto, sh, totalAdjustmentAmnt, rownum, adjustmentRecord);
                sh = checkAndReset(wb, sh, rownum);
            }
        }
        sh = checkAndReset(wb, sh, rownum);
        createTotalAdjustmentAmountRow(wb, sh, totalAdjustmentAmnt, rownum);
        return checkAndReset(wb, sh, rownum);
    }

    private Sheet addIndent(SXSSFWorkbook wb, Sheet sh, AtomicInteger rownum) {
        sh.createRow(rownum.getAndIncrement());
        sh = checkAndReset(wb, sh, rownum);
        sh.createRow(rownum.getAndIncrement());
        return checkAndReset(wb, sh, rownum);
    }

    private Sheet checkAndReset(SXSSFWorkbook wb, Sheet sh, AtomicInteger rownum) {
        if (rownum.get() >= limit) {
            sh = createSheet(wb);
            rownum.set(0);
        }
        return sh;
    }

    private Sheet createSheet(SXSSFWorkbook wb) {
        Sheet sh = wb.createSheet();
        sh.setDefaultColumnWidth(20);
        return sh;
    }

    private void createTotalRefundAmountRow(Workbook wb, Sheet sh, Cash totalRefundAmnt, AtomicInteger rownum) {
        Row rowTotalRefundAmount = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < 4; ++i) {
            Cell cell = rowTotalRefundAmount.createCell(i);
            cell.setCellStyle(createGreyCellStyle(wb));
            CellUtil.setFont(cell, createBoldFont(wb));
        }
        sh.addMergedRegion(new CellRangeAddress(rownum.get() - 1, rownum.get() - 1, 0, 2));
        Cell cellTotalRefundAmount = rowTotalRefundAmount.getCell(0);
        cellTotalRefundAmount.setCellValue("Сумма");
        CellUtil.setAlignment(cellTotalRefundAmount, HorizontalAlignment.CENTER);
        rowTotalRefundAmount.getCell(3).setCellValue(
                FormatUtil.formatCurrency(totalRefundAmnt.getAmount().get(),
                        totalRefundAmnt.getCurrency().getExponent()));
    }

    private void createRefundRow(LocalReportCreatorDto reportCreatorDto,
                                 Sheet sh,
                                 Cash totalRefundAmnt,
                                 AtomicInteger rownum,
                                 RefundRecord refund) {
        ZoneId reportZoneId = ZoneId.of(reportCreatorDto.getReport().getTimezone());

        Row row = sh.createRow(rownum.getAndIncrement());
        PaymentRecord payment = localStatisticService.getCapturedPayment(
                reportCreatorDto.getReport().getPartyId(),
                reportCreatorDto.getReport().getPartyShopId(),
                refund.getInvoiceId(),
                refund.getPaymentId()
        );
        row.createCell(0).setCellValue(
                TimeUtil.toLocalizedDateTime(refund.getStatusCreatedAt().toInstant(ZoneOffset.UTC), reportZoneId));
        row.createCell(1).setCellValue(
                TimeUtil.toLocalizedDateTime(payment.getStatusCreatedAt().toInstant(ZoneOffset.UTC),
                        reportZoneId));
        row.createCell(2).setCellValue(refund.getInvoiceId() + "." + refund.getPaymentId());
        var currency = dominantService.getCurrency(refund.getCurrencyCode());
        row.createCell(3).setCellValue(FormatUtil.formatCurrency(refund.getAmount(), currency.getExponent()));
        String paymentTool = null;
        String payerEmail = null;
        if (payment.getPayerType() == PaymentPayerType.payment_resource) {
            paymentTool = PaymentPayerType.payment_resource.getLiteral();
            payerEmail = payment.getEmail();
        }
        row.createCell(4).setCellValue(paymentTool);
        totalRefundAmnt.setCurrency(currency);
        totalRefundAmnt.getAmount().addAndGet(refund.getAmount());
        row.createCell(5).setCellValue(payerEmail);
        row.createCell(6).setCellValue(reportCreatorDto.getShopUrls().get(refund.getShopId()));

        String purpose = localStatisticService.getPurpose(refund.getInvoiceId());
        row.createCell(7).setCellValue(purpose);
        row.createCell(8).setCellValue(refund.getRefundId());
        row.createCell(9).setCellValue(refund.getReason());
        row.createCell(10).setCellValue(refund.getCurrencyCode());
        row.createCell(11).setCellValue(refund.getExternalId());
        row.createCell(12).setCellValue(refund.getStatus().getLiteral());
        row.createCell(13).setCellValue(refund.getShopId());
        row.createCell(14).setCellValue(reportCreatorDto.getShopNames().get(refund.getShopId()));
    }

    private void createRefundsColumnsDescriptionRow(Workbook wb, Sheet sh, AtomicInteger rownum) {
        Row rowSecondRefunds = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < REFUNDS_COLUMNS_DESCRIPTION_ROW; ++i) {
            Cell cell = rowSecondRefunds.createCell(i);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            cell.setCellStyle(createGreyCellStyle(wb));
            CellUtil.setFont(cell, createBoldFont(wb));
        }
        rowSecondRefunds.getCell(0).setCellValue("Дата возврата");
        rowSecondRefunds.getCell(1).setCellValue("Дата платежа");
        rowSecondRefunds.getCell(2).setCellValue("Id платежа");
        rowSecondRefunds.getCell(3).setCellValue("Сумма возврата");
        rowSecondRefunds.getCell(4).setCellValue("Метод оплаты");
        rowSecondRefunds.getCell(5).setCellValue("Email плательщика");
        rowSecondRefunds.getCell(6).setCellValue("URL магазина");
        rowSecondRefunds.getCell(7).setCellValue("Назначение платежа");
        rowSecondRefunds.getCell(8).setCellValue("Id возврата");
        rowSecondRefunds.getCell(9).setCellValue("Причина возврата");
        rowSecondRefunds.getCell(10).setCellValue("Валюта");
        rowSecondRefunds.getCell(11).setCellValue("Id мерчанта");
        rowSecondRefunds.getCell(12).setCellValue("Статус возврата");
        rowSecondRefunds.getCell(13).setCellValue("Shop Id");
        rowSecondRefunds.getCell(14).setCellValue("Название магазина");
    }

    private void createRefundsHeadRow(LocalReportCreatorDto reportCreatorDto,
                                      Workbook wb,
                                      Sheet sh,
                                      AtomicInteger rownum) {
        Row rowFirstRefunds = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < REFUNDS_HEAD_ROW; ++i) {
            rowFirstRefunds.createCell(i);
        }
        sh.addMergedRegion(new CellRangeAddress(rownum.get() - 1, rownum.get() - 1, 0, 7));
        Cell cellFirstRefunds = rowFirstRefunds.getCell(0);
        cellFirstRefunds.setCellValue(String.format("Возвраты за период с %s по %s",
                reportCreatorDto.getFromTime(), reportCreatorDto.getToTime()));
        CellUtil.setAlignment(cellFirstRefunds, HorizontalAlignment.CENTER);
        CellUtil.setFont(cellFirstRefunds, createBoldFont(wb));
    }

    private void createAdjustmentColumnsDescriptionRow(Workbook wb,
                                                       Sheet sh,
                                                       AtomicInteger rownum) {
        Row rowSecondRefunds = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < ADJUSTMENT_COLUMNS_DESCRIPTION_ROW; ++i) {
            Cell cell = rowSecondRefunds.createCell(i);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            cell.setCellStyle(createGreyCellStyle(wb));
            CellUtil.setFont(cell, createBoldFont(wb));
        }
        rowSecondRefunds.getCell(0).setCellValue("Id корректировки");
        rowSecondRefunds.getCell(1).setCellValue("Id платежа");
        rowSecondRefunds.getCell(2).setCellValue("Дата корректировки");
        rowSecondRefunds.getCell(3).setCellValue("Сумма");
        rowSecondRefunds.getCell(4).setCellValue("Валюта");
        rowSecondRefunds.getCell(5).setCellValue("Причина корректировки");
        rowSecondRefunds.getCell(6).setCellValue("Статус корректировки");
        rowSecondRefunds.getCell(7).setCellValue("Shop Id");
        rowSecondRefunds.getCell(8).setCellValue("Название магазина");
    }

    private void createAdjustmentsHeadRow(LocalReportCreatorDto reportCreatorDto,
                                          SXSSFWorkbook wb,
                                          Sheet sh,
                                          AtomicInteger rownum) {
        Row rowFirstAdjustments = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < ADJUSTMENT_HEAD_ROW; ++i) {
            rowFirstAdjustments.createCell(i);
        }
        sh.addMergedRegion(new CellRangeAddress(rownum.get() - 1, rownum.get() - 1, 0, 7));
        Cell cellFirstRefunds = rowFirstAdjustments.getCell(0);
        cellFirstRefunds.setCellValue(
                String.format("Корректировки за период с %s по %s", reportCreatorDto.getFromTime(),
                        reportCreatorDto.getToTime()));
        CellUtil.setAlignment(cellFirstRefunds, HorizontalAlignment.CENTER);
        CellUtil.setFont(cellFirstRefunds, createBoldFont(wb));
    }

    private void createAdjustmentRow(LocalReportCreatorDto reportCreatorDto,
                                     Sheet sh,
                                     Cash totalAdjustmentAmnt,
                                     AtomicInteger rownum,
                                     AdjustmentRecord adjustment) {
        ZoneId reportZoneId = ZoneId.of(reportCreatorDto.getReport().getTimezone());

        Row row = sh.createRow(rownum.getAndIncrement());
        row.createCell(0).setCellValue(adjustment.getAdjustmentId());
        row.createCell(1).setCellValue(adjustment.getInvoiceId() + "." + adjustment.getPaymentId());
        row.createCell(2).setCellValue(
                TimeUtil.toLocalizedDateTime(adjustment.getStatusCreatedAt().toInstant(ZoneOffset.UTC), reportZoneId));
        var currency = dominantService.getCurrency(adjustment.getCurrencyCode());
        row.createCell(3).setCellValue(FormatUtil.formatCurrency(adjustment.getAmount(), currency.getExponent()));
        totalAdjustmentAmnt.getAmount().addAndGet(adjustment.getAmount());
        row.createCell(4).setCellValue(adjustment.getCurrencyCode());
        row.createCell(5).setCellValue(adjustment.getReason());
        row.createCell(6).setCellValue(adjustment.getStatus().getLiteral());
        row.createCell(7).setCellValue(adjustment.getShopId());
        row.createCell(8).setCellValue(reportCreatorDto.getShopNames().get(adjustment.getShopId()));
    }

    private void createTotalAdjustmentAmountRow(SXSSFWorkbook wb,
                                                Sheet sh,
                                                Cash totalAdjustmentAmnt,
                                                AtomicInteger rownum) {
        Row rowTotalPaymentAmount = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < 6; ++i) {
            Cell cell = rowTotalPaymentAmount.createCell(i);
            cell.setCellStyle(createGreyCellStyle(wb));
            CellUtil.setFont(cell, createBoldFont(wb));
        }
        sh.addMergedRegion(new CellRangeAddress(rownum.get() - 1, rownum.get() - 1, 0, 2));
        Cell cellTotalPaymentAmount = rowTotalPaymentAmount.getCell(0);
        cellTotalPaymentAmount.setCellValue("Сумма");
        CellUtil.setAlignment(cellTotalPaymentAmount, HorizontalAlignment.CENTER);
        rowTotalPaymentAmount.getCell(3).setCellValue(
                FormatUtil.formatCurrency(totalAdjustmentAmnt.getAmount().longValue(),
                        totalAdjustmentAmnt.getCurrency().getExponent()));
    }

    private void createTotalAmountRow(Workbook wb,
                                      Sheet sh,
                                      Cash totalAmnt,
                                      AtomicInteger rownum) {
        Row rowTotalPaymentAmount = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < 5; ++i) {
            Cell cell = rowTotalPaymentAmount.createCell(i);
            cell.setCellStyle(createGreyCellStyle(wb));
            CellUtil.setFont(cell, createBoldFont(wb));
        }
        sh.addMergedRegion(new CellRangeAddress(rownum.get() - 1, rownum.get() - 1, 0, 2));
        Cell cellTotalPaymentAmount = rowTotalPaymentAmount.getCell(0);
        cellTotalPaymentAmount.setCellValue("Сумма");
        CellUtil.setAlignment(cellTotalPaymentAmount, HorizontalAlignment.CENTER);
        rowTotalPaymentAmount.getCell(3).setCellValue(
                FormatUtil.formatCurrency(totalAmnt.getAmount().get(), totalAmnt.getCurrency().getExponent()));
    }

    private void createPaymentRow(LocalReportCreatorDto reportCreatorDto,
                                  Sheet sh,
                                  Cash totalAmnt,
                                  AtomicInteger rownum,
                                  PaymentRecord payment) {
        ZoneId reportZoneId = ZoneId.of(reportCreatorDto.getReport().getTimezone());
        Row row = sh.createRow(rownum.getAndIncrement());
        row.createCell(0).setCellValue(payment.getInvoiceId() + "." + payment.getPaymentId());
        row.createCell(1).setCellValue(
                TimeUtil.toLocalizedDateTime(payment.getStatusCreatedAt().toInstant(ZoneOffset.UTC), reportZoneId));
        row.createCell(2).setCellValue(payment.getTool().getName());
        var currency = dominantService.getCurrency(payment.getCurrencyCode());
        row.createCell(3).setCellValue(FormatUtil.formatCurrency(payment.getAmount(), currency.getExponent()));
        long fee = Objects.nonNull(payment.getFee()) ? payment.getFee() : 0L;
        row.createCell(4).setCellValue(
                FormatUtil.formatCurrency(payment.getAmount() - fee, currency.getExponent()));
        totalAmnt.setCurrency(currency);
        totalAmnt.getAmount().addAndGet(payment.getAmount());
        row.createCell(5).setCellValue(payment.getEmail());
        row.createCell(6).setCellValue(reportCreatorDto.getShopUrls().get(payment.getShopId()));

        var invoice = localStatisticService.getInvoice(payment.getInvoiceId());
        row.createCell(7).setCellValue(invoice.getProduct());
        row.createCell(8).setCellValue(FormatUtil.formatCurrency(fee, currency.getExponent()));
        row.createCell(9).setCellValue(payment.getCurrencyCode());
        if (StringUtils.hasText(payment.getExternalId())) {
            row.createCell(10).setCellValue(payment.getExternalId());
        } else {
            row.createCell(10).setCellValue(invoice.getExternalId());
        }
        row.createCell(11).setCellValue(payment.getStatus().getLiteral());
        row.createCell(12).setCellValue(payment.getShopId());
        row.createCell(13).setCellValue(reportCreatorDto.getShopNames().get(payment.getShopId()));
    }

    private void createPaymentsColumnsDescriptionRow(Workbook wb, Sheet sh, AtomicInteger rownum) {
        Row rowSecondPayments = sh.createRow(rownum.getAndIncrement());
        for (int i = 0; i < PAYMENTS_COLUMNS_DESCRIPTION_ROW; ++i) {
            Cell cell = rowSecondPayments.createCell(i);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            cell.setCellStyle(createGreyCellStyle(wb));
            CellUtil.setFont(cell, createBoldFont(wb));
        }
        rowSecondPayments.getCell(0).setCellValue("Id платежа");
        rowSecondPayments.getCell(1).setCellValue("Дата");
        rowSecondPayments.getCell(2).setCellValue("Метод оплаты");
        rowSecondPayments.getCell(3).setCellValue("Сумма платежа");
        rowSecondPayments.getCell(4).setCellValue("Сумма к выводу");
        rowSecondPayments.getCell(5).setCellValue("Email плательщика");
        rowSecondPayments.getCell(6).setCellValue("URL магазина");
        rowSecondPayments.getCell(7).setCellValue("Назначение платежа");
        rowSecondPayments.getCell(8).setCellValue("Комиссия");
        rowSecondPayments.getCell(9).setCellValue("Валюта");
        rowSecondPayments.getCell(10).setCellValue("Id мерчанта");
        rowSecondPayments.getCell(11).setCellValue("Статус платежа");
        rowSecondPayments.getCell(12).setCellValue("Shop Id");
        rowSecondPayments.getCell(13).setCellValue("Название магазина");
    }

    private void createPaymentsHeadRow(LocalReportCreatorDto reportCreatorDto,
                                       Workbook wb,
                                       Sheet sh,
                                       AtomicInteger rownum) {
        Row rowFirstPayments = sh.createRow(rownum.getAndIncrement());

        for (int i = 0; i < PAYMENTS_HEAD_ROW; ++i) {
            rowFirstPayments.createCell(i);
        }
        sh.addMergedRegion(new CellRangeAddress(rownum.get() - 1, rownum.get() - 1, 0, 7));
        Cell cellFirstPayments = rowFirstPayments.getCell(0);
        cellFirstPayments.setCellValue(String.format("Платежи за период с %s по %s",
                reportCreatorDto.getFromTime(), reportCreatorDto.getToTime()));
        CellUtil.setAlignment(cellFirstPayments, HorizontalAlignment.CENTER);
        CellUtil.setFont(cellFirstPayments, createBoldFont(wb));
    }

    private CellStyle createGreyCellStyle(Workbook wb) {
        CellStyle greyStyle = wb.createCellStyle();
        greyStyle.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        greyStyle.setFillPattern(FillPatternType.LESS_DOTS);
        return greyStyle;
    }

    private Font createBoldFont(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        return font;
    }

}

