package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.model.ReportCreatorDto;

import java.io.IOException;

public interface ReportCreatorService {
    void createReport(ReportCreatorDto reportCreatorDto) throws IOException;
}
