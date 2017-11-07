package com.rbkmoney.reporter.service;

import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.reporter.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.exception.ReportNotFoundException;
import com.rbkmoney.reporter.model.PartyModel;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static com.rbkmoney.reporter.util.TimeUtil.toZoneSameLocal;

@Service
public class TemplateService {

    private final PartyService partyService;

    private final StatisticService statisticService;

    @Autowired
    public TemplateService(PartyService partyService, StatisticService statisticService) {
        this.partyService = partyService;
        this.statisticService = statisticService;
    }

    public void processPaymentRegistryTemplate(List<StatPayment> payments, Instant fromTime, Instant toTime, OutputStream outputStream) throws IOException {
        Context context = new Context();
        context.putVar("payments", payments);
        context.putVar("fromTime", Date.from(fromTime));
        context.putVar("toTime", Date.from(toTime));

        processTemplate(context, ReportType.payment_registry, outputStream);
    }

    public void processProvisionOfServiceTemplate(PartyModel partyModel, ShopAccountingModel shopAccountingModel, Instant fromTime, Instant toTime, ZoneId zoneId, OutputStream outputStream) throws IOException {
        Context context = new Context();
        context.putVar("shopAccounting", shopAccountingModel);
        context.putVar("partyRepresentation", partyModel);
        context.putVar("fromTime", Date.from(toZoneSameLocal(fromTime, zoneId)));
        context.putVar("toTime", Date.from(toZoneSameLocal(toTime, zoneId).minusMillis(1)));

        processTemplate(context, ReportType.provision_of_service, outputStream);
    }

    public void processReportTemplate(Report report, OutputStream outputStream) throws IOException {
        Instant fromTime = report.getFromTime().toInstant(ZoneOffset.UTC);
        Instant toTime = report.getToTime().toInstant(ZoneOffset.UTC);
        Instant createdAt = report.getCreatedAt().toInstant(ZoneOffset.UTC);

        ReportType reportType = ReportType.valueOf(report.getType());

        PartyModel partyModel = partyService.getPartyRepresentation(
                report.getPartyId(),
                report.getPartyShopId(),
                createdAt
        );

        switch (reportType) {
            case provision_of_service:
                ShopAccountingModel shopAccountingModel = statisticService.getShopAccounting(
                        report.getPartyId(),
                        report.getPartyShopId(),
                        fromTime,
                        toTime
                );
                processProvisionOfServiceTemplate(partyModel, shopAccountingModel, fromTime, toTime, ZoneId.of(report.getTimezone()), outputStream);
                break;
            case payment_registry:
                List<StatPayment> payments = statisticService.getPayments(
                        report.getPartyId(),
                        report.getPartyShopId(),
                        fromTime,
                        toTime
                );
                processPaymentRegistryTemplate(payments, fromTime, toTime, outputStream);
                break;
            default:
                throw new ReportNotFoundException(String.format("Unknown report type, reportType='%s'", reportType));
        }
    }

    public void processTemplate(Context context, ReportType reportType, OutputStream outputStream) throws IOException {
        processTemplate(context, reportType.getTemplateResource().getInputStream(), outputStream);
    }

    public void processTemplate(Context context, InputStream inputStream, OutputStream outputStream) throws IOException {
        JxlsHelper.getInstance()
                .processTemplate(
                        inputStream,
                        outputStream,
                        context
                );
    }

}
