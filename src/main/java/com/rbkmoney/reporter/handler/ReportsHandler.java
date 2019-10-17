package com.rbkmoney.reporter.handler;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.reports.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.enums.ReportStatus;
import com.rbkmoney.reporter.exception.FileNotFoundException;
import com.rbkmoney.reporter.exception.PartyNotFoundException;
import com.rbkmoney.reporter.exception.ReportNotFoundException;
import com.rbkmoney.reporter.exception.ShopNotFoundException;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.ReportService;
import com.rbkmoney.reporter.util.DamselUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.rbkmoney.reporter.util.DamselUtil.buildInvalidRequest;

/**
 * Created by tolkonepiu on 18/07/2017.
 */
@Slf4j
@Component
public class ReportsHandler implements ReportingSrv.Iface {

    private final ReportService reportService;

    private final PartyService partyService;

    @Autowired
    public ReportsHandler(ReportService reportService, PartyService partyService) {
        this.reportService = reportService;
        this.partyService = partyService;
    }

    @Override
    public List<Report> getReports(ReportRequest reportRequest, List<ReportType> reportTypes) throws DatasetTooBig, InvalidRequest, TException {
        try {
            Instant fromTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getFromTime());
            Instant toTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getToTime());

            if (fromTime.compareTo(toTime) > 0) {
                throw buildInvalidRequest("fromTime must be less that toTime");
            }

            return reportService.getReportsByRange(
                    reportRequest.getPartyId(),
                    reportRequest.getShopId(),
                    reportTypes.stream()
                            .map(reportType -> TypeUtil.toEnumField(reportType.name(), com.rbkmoney.reporter.domain.enums.ReportType.class))
                            .collect(Collectors.toList()),
                    fromTime,
                    toTime
            ).stream()
                    .filter(report -> report.getStatus() != ReportStatus.cancelled)
                    .map(report -> DamselUtil.toDamselReport(report, reportService.getReportFiles(report.getId())))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException ex) {
            throw buildInvalidRequest(ex);
        }
    }

    @Override
    public long generateReport(ReportRequest reportRequest, ReportType reportType) throws PartyNotFound, ShopNotFound, InvalidRequest, TException {
        try {

            Instant fromTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getFromTime());
            Instant toTime = TypeUtil.stringToInstant(reportRequest.getTimeRange().getToTime());

            if (fromTime.compareTo(toTime) > 0) {
                throw buildInvalidRequest("fromTime must be less that toTime");
            }

            partyService.getShop(reportRequest.getPartyId(), reportRequest.getShopId());
            return reportService.createReport(
                    reportRequest.getPartyId(),
                    reportRequest.getShopId(),
                    fromTime,
                    toTime,
                    TypeUtil.toEnumField(reportType.name(), com.rbkmoney.reporter.domain.enums.ReportType.class)
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
    public Report getReport(String partyId, String shopId, long reportId) throws ReportNotFound, TException {
        try {
            com.rbkmoney.reporter.domain.tables.pojos.Report report = reportService.getReport(reportId, false);
            if (partyId == null || !partyId.equals(report.getPartyId())) {
                log.warn("Not enough rights to execute the report {} for party id {}", reportId, partyId);
                throw new ReportNotFoundException();
            }
            return DamselUtil.toDamselReport(report, reportService.getReportFiles(reportId));
        } catch (ReportNotFoundException ex) {
            throw new ReportNotFound();
        }
    }

    @Override
    public void cancelReport(String partyId, String shopId, long reportId) throws ReportNotFound, TException {
        try {
            reportService.cancelReport(partyId, shopId, reportId);
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
}
