package dev.vality.reporter.handler.report;

import dev.vality.geck.common.util.TypeUtil;
import dev.vality.reporter.*;
import dev.vality.reporter.domain.enums.ReportType;
import dev.vality.reporter.exception.FileNotFoundException;
import dev.vality.reporter.exception.PartyNotFoundException;
import dev.vality.reporter.exception.ReportNotFoundException;
import dev.vality.reporter.exception.ShopNotFoundException;
import dev.vality.reporter.service.PartyService;
import dev.vality.reporter.service.ReportNewProtoService;
import dev.vality.reporter.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.vality.reporter.util.NewProtoUtil.buildInvalidRequest;
import static dev.vality.reporter.util.NewProtoUtil.toNewProtoReport;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportsNewProtoHandler implements ReportingSrv.Iface {

    private static final int REPORTS_LIMIT = 100;

    private final PartyService partyService;
    private final ReportNewProtoService reportService;

    @Override
    public long createReport(ReportRequest reportRequest, String reportType)
            throws TException {
        try {
            Instant fromTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getFromTime());
            Instant toTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getToTime());

            if (fromTime.compareTo(toTime) > 0) {
                throw buildInvalidRequest("fromTime must be less that toTime");
            }

            if (reportRequest.getShopId() != null) {
                partyService.getShop(reportRequest.getPartyId(), reportRequest.getShopId());
            }

            return reportService.createReport(
                    reportRequest.getPartyId(),
                    reportRequest.getShopId(),
                    fromTime,
                    toTime,
                    ReportType.valueOf(reportType)
            );
        } catch (PartyNotFoundException ex) {
            throw new PartyNotFound();
        } catch (ShopNotFoundException ex) {
            throw new ShopNotFound();
        } catch (IllegalArgumentException ex) {
            throw buildInvalidRequest(ex);
        }
    }

    @Override
    public StatReportResponse getReports(StatReportRequest statReportRequest)
            throws TException {
        try {
            ReportRequest reportRequest = statReportRequest.getRequest();
            List<String> reportTypes = statReportRequest.getReportTypes();
            String continuationToken = statReportRequest.getContinuationToken();
            Instant fromTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getFromTime());
            Instant toTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getToTime());
            int limit = statReportRequest.isSetLimit() ? statReportRequest.getLimit() : REPORTS_LIMIT;

            if (fromTime.compareTo(toTime) > 0) {
                throw buildInvalidRequest("fromTime must be less that toTime");
            }

            if (statReportRequest.isSetContinuationToken() && !TokenUtil.isValid(statReportRequest)) {
                throw new BadToken();
            }

            List<dev.vality.reporter.domain.tables.pojos.Report> reports = reportService.getReportsWithToken(
                    reportRequest.getPartyId(),
                    getShopIds(reportRequest),
                    reportTypes != null ? reportTypes(reportTypes) : null,
                    fromTime,
                    toTime,
                    statReportRequest.isSetContinuationToken()
                            ? TypeUtil.stringToInstant(TokenUtil.extractTime(continuationToken))
                            : null,
                    limit
            );
            List<Report> reportsFiltered = reports.stream()
                    .filter(report -> report.getStatus() != dev.vality.reporter.domain.enums.ReportStatus.cancelled)
                    .map(report -> toNewProtoReport(report, reportService.getReportFiles(report.getId())))
                    .collect(Collectors.toList());

            StatReportResponse statReportResponse = new StatReportResponse(reportsFiltered);
            if (reports.size() >= limit) {
                String nextCreatedAfter = TypeUtil.temporalToString(reports.get(reports.size() - 1).getCreatedAt());
                statReportResponse
                        .setContinuationToken(TokenUtil.buildToken(reportRequest, reportTypes, nextCreatedAfter));
            }
            return statReportResponse;
        } catch (IllegalArgumentException ex) {
            throw buildInvalidRequest(ex);
        }
    }

    private List<String> getShopIds(ReportRequest reportRequest) {
        return reportRequest.isSetShopIds()
                ? reportRequest.getShopIds()
                : getShopId(reportRequest);
    }

    private List<String> getShopId(ReportRequest reportRequest) {
        return Optional.ofNullable(reportRequest.getShopId())
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    @Override
    public Report getReport(long reportId) throws TException {
        try {
            return toNewProtoReport(
                    reportService.getReport(reportId, false),
                    reportService.getReportFiles(reportId)
            );
        } catch (ReportNotFoundException ex) {
            throw new ReportNotFound();
        }
    }

    @Override
    public void cancelReport(long reportId) throws TException {
        try {
            reportService.cancelReport(reportId);
        } catch (ReportNotFoundException ex) {
            throw new ReportNotFound();
        }
    }

    @Override
    public String generatePresignedUrl(String fileId, String expiresIn)
            throws TException {
        try {
            Instant expiresInInstant = TypeUtil.stringToInstant(expiresIn);
            URL url = reportService.generatePresignedUrl(fileId, expiresInInstant);
            return url.toString();
        } catch (FileNotFoundException ex) {
            throw new FileNotFound();
        } catch (IllegalArgumentException ex) {
            throw buildInvalidRequest(ex);
        }
    }

    private List<ReportType> reportTypes(List<String> reportTypes) {
        return reportTypes.stream()
                .map(ReportType::valueOf)
                .collect(Collectors.toList());
    }
}
