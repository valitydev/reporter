package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Service
public interface TemplateService {

    default boolean accept(ReportType reportType) {
        return getReportTypes().contains(reportType);
    }

    void processReportTemplate(Report report, OutputStream outputStream) throws IOException;

    List<ReportType> getReportTypes();

    default void processTemplate(Context context, InputStream templateStream, OutputStream outputStream) throws IOException {
        JxlsHelper.getInstance()
                .processTemplate(
                        templateStream,
                        outputStream,
                        context
                );
    }

}
