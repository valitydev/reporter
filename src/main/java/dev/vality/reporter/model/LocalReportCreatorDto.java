package dev.vality.reporter.model;

import dev.vality.reporter.domain.tables.pojos.Report;
import dev.vality.reporter.domain.tables.records.AdjustmentRecord;
import dev.vality.reporter.domain.tables.records.PaymentRecord;
import dev.vality.reporter.domain.tables.records.RefundRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jooq.Cursor;

import java.io.OutputStream;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class LocalReportCreatorDto {

    private String fromTime;
    private String toTime;
    private Cursor<PaymentRecord> paymentsCursor;
    private Cursor<RefundRecord> refundsCursor;
    private Cursor<AdjustmentRecord> adjustmentsCursor;
    private Report report;
    private OutputStream outputStream;
    private Map<String, String> shopUrls;

}
