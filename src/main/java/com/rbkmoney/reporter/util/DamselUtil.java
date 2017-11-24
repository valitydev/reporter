package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.reports.*;
import com.rbkmoney.geck.common.util.TypeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DamselUtil {

    public static Report toDamselReport(com.rbkmoney.reporter.domain.tables.pojos.Report report, List<com.rbkmoney.reporter.domain.tables.pojos.FileMeta> files) throws IllegalArgumentException {
        Report dReport = new Report();
        dReport.setReportId(report.getId());
        dReport.setStatus(ReportStatus.valueOf(report.getStatus().getLiteral()));
        ReportTimeRange timeRange = new ReportTimeRange(
                TypeUtil.temporalToString(report.getFromTime()),
                TypeUtil.temporalToString(report.getToTime())
        );
        dReport.setTimeRange(timeRange);
        dReport.setReportType(ReportType.valueOf(report.getType()));
        dReport.setCreatedAt(TypeUtil.temporalToString(report.getCreatedAt()));

        dReport.setFiles(files.stream()
                .map(DamselUtil::toDamselFile)
                .collect(Collectors.toList()));

        return dReport;
    }

    public static FileMeta toDamselFile(com.rbkmoney.reporter.domain.tables.pojos.FileMeta file) {
        FileMeta fileMeta = new FileMeta();
        fileMeta.setFileId(file.getFileId());
        fileMeta.setFilename(file.getFilename());
        Signature signature = new Signature();
        signature.setMd5(file.getMd5());
        signature.setSha256(file.getSha256());
        fileMeta.setSignature(signature);
        return fileMeta;
    }

    public static InvalidRequest buildInvalidRequest(Throwable throwable) {
        return buildInvalidRequest(throwable.getMessage());
    }

    public static InvalidRequest buildInvalidRequest(String... messages) {
        return new InvalidRequest(Arrays.asList(messages));
    }

}
