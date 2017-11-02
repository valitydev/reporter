package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.AbstractIntegrationTest;
import com.rbkmoney.reporter.model.PartyModel;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import static com.rbkmoney.reporter.util.TimeUtil.toZoneSameLocal;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

/**
 * Created by tolkonepiu on 12/07/2017.
 */
public class TemplateServiceTest extends AbstractIntegrationTest {

    @Autowired
    private TemplateService templateService;

    @Test
    public void generateProvisionOfServiceReportTest() throws IOException {
        Path tempFile = Files.createTempFile("provision_of_service_", "_test_report.xlsx");
        System.out.println("Provision of service report generated on " + tempFile.toAbsolutePath().toString());

        Instant fromTime = random(Instant.class);
        Instant toTime = random(Instant.class);
        PartyModel partyModel = random(PartyModel.class);
        ShopAccountingModel shopAccountingModel = random(ShopAccountingModel.class);
        ZoneId zoneId = ZoneId.of("Europe/Moscow");

        try {
            templateService.processProvisionOfServiceTemplate(
                    partyModel,
                    shopAccountingModel,
                    fromTime,
                    toTime,
                    zoneId,
                    Files.newOutputStream(tempFile));

            Workbook wb = new XSSFWorkbook(Files.newInputStream(tempFile));
            Sheet sheet = wb.getSheetAt(0);

            Row headerRow = sheet.getRow(1);
            Cell merchantContractIdCell = headerRow.getCell(0);
            assertEquals(
                    String.format("к Договору № %s от", partyModel.getMerchantContractId()),
                    merchantContractIdCell.getStringCellValue()
            );
            Cell merchantContractCreatedAtCell = headerRow.getCell(3);
            assertEquals("dd\\.mm\\.yyyy", merchantContractCreatedAtCell.getCellStyle().getDataFormatString());
            assertEquals(
                    partyModel.getMerchantContractCreatedAt(),
                    merchantContractCreatedAtCell.getDateCellValue()
            );

            Cell merchantNameCell = sheet.getRow(5).getCell(4);
            assertEquals(partyModel.getMerchantName(), merchantNameCell.getStringCellValue());

            Cell merchantIdCell = sheet.getRow(7).getCell(4);
            assertEquals(partyModel.getMerchantId(), merchantIdCell.getStringCellValue());

            Row dateRow = sheet.getRow(14);
            Cell fromTimeCell = dateRow.getCell(1);
            assertEquals(
                    "[$-FC19]dd\\ mmmm\\ yyyy\\ \\г\\.;@",
                    fromTimeCell.getCellStyle().getDataFormatString()
            );
            assertEquals(Date.from(toZoneSameLocal(fromTime, zoneId)), fromTimeCell.getDateCellValue());
            Cell toTimeCell = dateRow.getCell(3);
            assertEquals(
                    "[$-FC19]dd\\ mmmm\\ yyyy\\ \\г\\.;@",
                    toTimeCell.getCellStyle().getDataFormatString()
            );
            assertEquals(Date.from(toZoneSameLocal(toTime, zoneId)), toTimeCell.getDateCellValue());

            Cell openingBalanceCell = sheet.getRow(23).getCell(3);
            assertEquals("#,##0.00", openingBalanceCell.getCellStyle().getDataFormatString());
            assertEquals(
                    BigDecimal.valueOf(shopAccountingModel.getOpeningBalance()),
                    BigDecimal.valueOf(openingBalanceCell.getNumericCellValue())
            );

            Cell fundsPaidOutCell = sheet.getRow(26).getCell(3);
            assertEquals("#,##0.00", fundsPaidOutCell.getCellStyle().getDataFormatString());
            assertEquals(
                    BigDecimal.valueOf(shopAccountingModel.getFundsPaidOut()),
                    BigDecimal.valueOf(fundsPaidOutCell.getNumericCellValue())
            );

            Cell fundsRefundedCell = sheet.getRow(28).getCell(3);
            assertEquals("#,##0.00", fundsRefundedCell.getCellStyle().getDataFormatString());
            assertEquals(
                    BigDecimal.valueOf(shopAccountingModel.getFundsRefunded()),
                    BigDecimal.valueOf(fundsRefundedCell.getNumericCellValue())
            );

            Cell closingBalanceCell = sheet.getRow(29).getCell(3);
            assertEquals("#,##0.00", closingBalanceCell.getCellStyle().getDataFormatString());
            assertEquals(
                    BigDecimal.valueOf(shopAccountingModel.getClosingBalance()),
                    BigDecimal.valueOf(closingBalanceCell.getNumericCellValue())
            );

            Cell fundsAcquiredCell = sheet.getRow(17).getCell(3);
            assertEquals("#,##0.00", fundsAcquiredCell.getCellStyle().getDataFormatString());
            assertEquals(
                    BigDecimal.valueOf(shopAccountingModel.getFundsAcquired()),
                    BigDecimal.valueOf(fundsAcquiredCell.getNumericCellValue())
            );
            assertEquals(
                    BigDecimal.valueOf(fundsAcquiredCell.getNumericCellValue()),
                    BigDecimal.valueOf(sheet.getRow(24).getCell(3).getNumericCellValue())
            );

            Cell feeChargedCell = sheet.getRow(19).getCell(3);
            assertEquals("#,##0.00", feeChargedCell.getCellStyle().getDataFormatString());
            assertEquals(
                    BigDecimal.valueOf(shopAccountingModel.getFeeCharged()),
                    BigDecimal.valueOf(feeChargedCell.getNumericCellValue())
            );
            assertEquals(
                    BigDecimal.valueOf(feeChargedCell.getNumericCellValue()),
                    BigDecimal.valueOf(sheet.getRow(25).getCell(3).getNumericCellValue())
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}
