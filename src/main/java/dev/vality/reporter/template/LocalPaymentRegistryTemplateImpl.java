package dev.vality.reporter.template;

import dev.vality.reporter.domain.enums.ReportType;
import dev.vality.reporter.domain.tables.pojos.Report;
import dev.vality.reporter.domain.tables.records.AdjustmentRecord;
import dev.vality.reporter.domain.tables.records.PaymentRecord;
import dev.vality.reporter.domain.tables.records.RefundRecord;
import dev.vality.reporter.model.LocalReportCreatorDto;
import dev.vality.reporter.service.LocalStatisticService;
import dev.vality.reporter.service.PartyService;
import dev.vality.reporter.service.ReportCreatorService;
import dev.vality.reporter.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Cursor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPaymentRegistryTemplateImpl implements ReportTemplate {

    private final PartyService partyService;

    private final ReportCreatorService<LocalReportCreatorDto> localReportCreatorService;

    private final LocalStatisticService localStatisticService;

    @Override
    public boolean isAccept(ReportType reportType) {
        return reportType == ReportType.payment_registry
                || reportType == ReportType.provision_of_service
                || reportType == ReportType.payment_registry_by_payout;
    }

    @Override
    public void processReportTemplate(Report report, OutputStream outputStream) throws
            IOException {
        log.info("Start process report template {}", report.getId());
        String partyId = report.getPartyId();
        String shopId = report.getPartyShopId();
        LocalDateTime fromTime = report.getFromTime();
        LocalDateTime toTime = report.getToTime();
        ZoneId reportZoneId = ZoneId.of(report.getTimezone());
        String formattedFromTime =
                TimeUtil.toLocalizedDate(fromTime.toInstant(ZoneOffset.UTC), reportZoneId);
        String formattedToTime =
                TimeUtil.toLocalizedDate(toTime.minusNanos(1).toInstant(ZoneOffset.UTC), reportZoneId);
        Map<String, String> shopUrls = partyService.getShopUrls(partyId);
        Map<String, String> shopNames = partyService.getShopNames(partyId);

        try (
                Cursor<PaymentRecord> paymentsCursor =
                        localStatisticService.getPaymentsCursor(partyId, shopId, fromTime, toTime);
                Cursor<RefundRecord> refundsCursor =
                        localStatisticService.getRefundsCursor(partyId, shopId, fromTime, toTime);
                Cursor<AdjustmentRecord> adjustmentCursor =
                        localStatisticService.getAdjustmentCursor(partyId, shopId, fromTime, toTime)
        ) {
            LocalReportCreatorDto reportCreatorDto = LocalReportCreatorDto.builder()
                    .fromTime(formattedFromTime)
                    .toTime(formattedToTime)
                    .paymentsCursor(paymentsCursor)
                    .refundsCursor(refundsCursor)
                    .adjustmentsCursor(adjustmentCursor)
                    .report(report)
                    .outputStream(outputStream)
                    .shopUrls(shopUrls)
                    .shopNames(shopNames)
                    .build();

            localReportCreatorService.createReport(reportCreatorDto);
            log.info("Report template {} were processed", report.getId());
        }
    }
}
