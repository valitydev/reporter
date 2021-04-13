package com.rbkmoney.reporter.handler.comparing;

import com.rbkmoney.reporter.dao.ReportComparingDataDao;
import com.rbkmoney.reporter.domain.enums.ComparingStatus;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.domain.tables.pojos.ReportComparingData;
import lombok.RequiredArgsConstructor;

import static com.rbkmoney.reporter.util.MapperUtils.createReportComparingData;

@RequiredArgsConstructor
public abstract class ReportComparingAbstractHandler implements ReportComparingHandler {

    private final ReportComparingDataDao reportComparingDataDao;

    @Override
    public abstract void compareReport(Report report);

    public void saveErrorComparingInfo(String failureReason, long reportId, ReportType reportType) {
        saveComparingInfo(reportId, reportType, ComparingStatus.FAILED, failureReason);
    }

    public void saveSuccessComparingInfo(long reportId, ReportType reportType) {
        saveComparingInfo(reportId, reportType, ComparingStatus.SUCCESS, null);
    }

    private void saveComparingInfo(long reportId,
                                   ReportType reportType,
                                   ComparingStatus status,
                                   String failureReason) {
        ReportComparingData reportComparingData = createReportComparingData(
                reportId,
                reportType,
                status,
                failureReason
        );
        reportComparingDataDao.saveReportComparingData(reportComparingData);
    }

}
