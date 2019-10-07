package com.rbkmoney.reporter.util;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NewProtoUtil {

    public static Report toNewProtoReport(com.rbkmoney.reporter.domain.tables.pojos.Report report, List<com.rbkmoney.reporter.domain.tables.pojos.FileMeta> files) throws IllegalArgumentException {
        ReportTimeRange timeRange = new ReportTimeRange(
                TypeUtil.temporalToString(report.getFromTime()),
                TypeUtil.temporalToString(report.getToTime())
        );

        Report newProtoReport = new Report();
        newProtoReport.setReportId(report.getId());
        newProtoReport.setPartyId(report.getPartyId());
        newProtoReport.setTimeRange(timeRange);
        newProtoReport.setCreatedAt(TypeUtil.temporalToString(report.getCreatedAt()));
        newProtoReport.setReportType(report.getType().getLiteral());
        newProtoReport.setStatus(ReportStatus.valueOf(report.getStatus().getLiteral()));
        newProtoReport.setFiles(fileMetas(files));
        newProtoReport.setShopId(report.getPartyShopId());

        return newProtoReport;
    }

    public static FileMeta toNewProtoFile(com.rbkmoney.reporter.domain.tables.pojos.FileMeta file) {
        Signature signature = new Signature();
        signature.setMd5(file.getMd5());
        signature.setSha256(file.getSha256());

        FileMeta fileMeta = new FileMeta();
        fileMeta.setFileId(file.getFileId());
        fileMeta.setFilename(file.getFilename());
        fileMeta.setSignature(signature);
        return fileMeta;
    }

    public static InvalidRequest buildInvalidRequest(Throwable throwable) {
        return buildInvalidRequest(throwable.getMessage());
    }

    public static InvalidRequest buildInvalidRequest(String... messages) {
        return new InvalidRequest(Arrays.asList(messages));
    }

    private static List<FileMeta> fileMetas(List<com.rbkmoney.reporter.domain.tables.pojos.FileMeta> files) {
        return files.stream()
                .map(NewProtoUtil::toNewProtoFile)
                .collect(Collectors.toList());
    }
}
