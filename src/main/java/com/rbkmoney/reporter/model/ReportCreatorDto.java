package com.rbkmoney.reporter.model;

import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.service.StatisticService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ReportCreatorDto {

    private String fromTime;
    private String toTime;
    private Iterator<StatPayment> paymentsIterator;
    private Iterator<StatRefund> refundsIterator;
    private Report report;
    private OutputStream outputStream;
    private Map<String, String> shopUrls;
    private Map<String, String> purposes;
    private StatisticService statisticService;
}
