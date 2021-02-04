package com.rbkmoney.reporter.handler.report;

import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.*;
import com.rbkmoney.reporter.base.InvalidRequest;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.exception.FileNotFoundException;
import com.rbkmoney.reporter.exception.PartyNotFoundException;
import com.rbkmoney.reporter.exception.ReportNotFoundException;
import com.rbkmoney.reporter.exception.ShopNotFoundException;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.ReportNewProtoService;
import com.rbkmoney.reporter.util.TokenUtil;
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

import static com.rbkmoney.reporter.util.NewProtoUtil.buildInvalidRequest;
import static com.rbkmoney.reporter.util.NewProtoUtil.toNewProtoReport;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportsNewProtoHandler implements ReportingSrv.Iface {

    private static final int REPORTS_LIMIT = 100;

    private final PartyService partyService;
    private final ReportNewProtoService reportService;

    @Override
    public long createReport(ReportRequest reportRequest, String reportType) throws PartyNotFound, ShopNotFound, InvalidRequest, TException {
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
    public StatReportResponse getReports(StatReportRequest statReportRequest) throws DatasetTooBig, InvalidRequest, BadToken, TException {
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

            List<com.rbkmoney.reporter.domain.tables.pojos.Report> reports = reportService.getReportsWithToken(
                    reportRequest.getPartyId(),
                    reportRequest.isSetShopIds()
                            ? reportRequest.getShopIds()
                            : Optional.ofNullable(reportRequest.getShopId()).map(Collections::singletonList).orElseGet(Collections::emptyList),
                    reportTypes != null ? reportTypes(reportTypes) : null,
                    fromTime,
                    toTime,
                    statReportRequest.isSetContinuationToken() ? TypeUtil.stringToInstant(TokenUtil.extractTime(continuationToken)) : null,
                    limit
            );
            List<Report> reportsFiltered = reports.stream()
                    .filter(report -> report.getStatus() != ReportStatus.cancelled)
                    .map(report -> toNewProtoReport(report, reportService.getReportFiles(report.getId())))
                    .collect(Collectors.toList());

            StatReportResponse statReportResponse = new StatReportResponse(reportsFiltered);
            if (reports.size() >= limit) {
                String nextCreatedAfter = TypeUtil.temporalToString(reports.get(reports.size() - 1).getCreatedAt());
                statReportResponse.setContinuationToken(TokenUtil.buildToken(reportRequest, reportTypes, nextCreatedAfter));
            }
            return statReportResponse;
        } catch (IllegalArgumentException ex) {
            throw buildInvalidRequest(ex);
        }
    }

    @Override
    public Report getReport(long reportId) throws ReportNotFound, TException {
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
    public void cancelReport(long reportId) throws ReportNotFound, TException {
        try {
            reportService.cancelReport(reportId);
        } catch (ReportNotFoundException ex) {
            throw new ReportNotFound();
        }
    }

    @Override
    public String generatePresignedUrl(String fileId, String expiresIn) throws FileNotFound, InvalidRequest, TException {
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
