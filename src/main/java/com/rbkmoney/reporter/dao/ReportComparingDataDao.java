package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.domain.tables.pojos.ReportComparingData;

import java.util.Optional;

public interface ReportComparingDataDao {

    Long saveReportComparingData(ReportComparingData reportComparingData);

    ReportComparingData getReportComparingDataByReportId(long reportId);

    Long getLastProcessedReport();

    Optional<Report> getNextComparingReport();

}
