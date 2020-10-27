package com.rbkmoney.reporter.service;

import java.io.IOException;

public interface ReportCreatorService<T> {

    void createReport(T reportCreatorDto) throws IOException;

}
