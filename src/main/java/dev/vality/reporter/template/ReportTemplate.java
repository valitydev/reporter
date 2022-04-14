package dev.vality.reporter.template;

import dev.vality.reporter.domain.enums.ReportType;
import dev.vality.reporter.domain.tables.pojos.Report;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

@Service
public interface ReportTemplate {

    boolean isAccept(ReportType reportType);

    void processReportTemplate(Report report, OutputStream outputStream) throws IOException;

}
