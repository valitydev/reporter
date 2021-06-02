package com.rbkmoney.reporter.template;

import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

@Service
public interface ReportTemplate {

    boolean isAccept(ReportType reportType);

    void processReportTemplate(Report report, OutputStream outputStream) throws IOException;

}
