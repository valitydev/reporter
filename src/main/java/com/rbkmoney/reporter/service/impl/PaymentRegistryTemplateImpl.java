package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.reporter.domain.enums.ReportType;
import com.rbkmoney.reporter.domain.tables.pojos.ContractMeta;
import com.rbkmoney.reporter.domain.tables.pojos.Report;
import com.rbkmoney.reporter.model.Payment;
import com.rbkmoney.reporter.model.Refund;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.StatisticService;
import com.rbkmoney.reporter.service.TemplateService;
import com.rbkmoney.reporter.util.FormatUtil;
import com.rbkmoney.reporter.util.TimeUtil;
import org.jxls.common.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PaymentRegistryTemplateImpl implements TemplateService {

    private final StatisticService statisticService;

    private final PartyService partyService;

    private final Resource paymentRegistry;

    private final Resource paymentRegistryWithoutRefunds;

    @Autowired
    public PaymentRegistryTemplateImpl(
            StatisticService statisticService,
            PartyService partyService,
            @Value("${report.type.pr.path|classpath:/templates/payment_registry.xlsx}") ClassPathResource paymentRegistry,
            @Value("${report.type.prwr.path|classpath:/templates/payment_registry_wo_refunds.xlsx}") ClassPathResource paymentRegistryWithoutRefunds
    ) {
        this.statisticService = statisticService;
        this.partyService = partyService;
        this.paymentRegistry = paymentRegistry;
        this.paymentRegistryWithoutRefunds = paymentRegistryWithoutRefunds;
    }

    @Override
    public boolean accept(ReportType reportType, ContractMeta contractMeta) {
        return reportType == ReportType.payment_registry
                || (contractMeta.getReportType() == ReportType.provision_of_service
                && partyService.needReference(contractMeta.getPartyId(), contractMeta.getContractId()));
    }

    @Override
    public void processReportTemplate(Report report, ContractMeta contractMeta, OutputStream outputStream) throws
            IOException {
        ZoneId reportZoneId = ZoneId.of(report.getTimezone());
        Map<String, String> shopUrls = partyService.getShopUrls(report.getPartyId(), report.getPartyContractId(), report.getCreatedAt().toInstant(ZoneOffset.UTC));

        Instant fromTime = report.getFromTime().toInstant(ZoneOffset.UTC);
        Instant toTime = report.getToTime().toInstant(ZoneOffset.UTC);
        Map<String, String> purposes = statisticService.getInvoices(
                report.getPartyId(),
                report.getPartyContractId(),
                fromTime,
                toTime
        ).stream().collect(Collectors.toMap(StatInvoice::getId, StatInvoice::getProduct));

        AtomicLong totalAmnt = new AtomicLong();
        AtomicLong totalPayoutAmnt = new AtomicLong();

        Stream<StatPayment> statPaymentsStream = statisticService.getPayments(
                report.getPartyId(),
                report.getPartyContractId(),
                fromTime,
                toTime,
                InvoicePaymentStatus.captured(new InvoicePaymentCaptured())
        ).stream();

        Stream<StatPayment> statPaymentsRefundedStream = statisticService.getPayments(
                report.getPartyId(),
                report.getPartyContractId(),
                fromTime,
                toTime,
                InvoicePaymentStatus.refunded(new InvoicePaymentRefunded()))
                .stream()
                .filter(p -> {
                    Instant createdAt = TypeUtil.stringToInstant(p.getCreatedAt());
                    return createdAt.isAfter(fromTime) && createdAt.isBefore(toTime);
                });

        List<Payment> paymentList = Stream.concat(statPaymentsStream, statPaymentsRefundedStream)
                .sorted(Comparator.comparing(this::getStatusChangedAt)).map(p -> {
                    Payment payment = new Payment();
                    payment.setId(p.getInvoiceId() + "." + p.getId());
                    payment.setCapturedAt(TimeUtil.toLocalizedDateTime(getStatusChangedAt(p), reportZoneId));
                    if (p.getPayer().isSetPaymentResource()) {
                        payment.setPaymentTool(p.getPayer().getPaymentResource().getPaymentTool().getSetField().getFieldName());
                    }
                    payment.setAmount(FormatUtil.formatCurrency(p.getAmount()));
                    payment.setPayoutAmount(FormatUtil.formatCurrency(p.getAmount() - p.getFee()));
                    totalAmnt.addAndGet(p.getAmount());
                    totalPayoutAmnt.addAndGet(p.getAmount() - p.getFee());
                    if (p.getPayer().isSetPaymentResource()) {
                        payment.setPayerEmail(p.getPayer().getPaymentResource().getEmail());
                    }
                    payment.setShopUrl(shopUrls.get(p.getShopId()));
                    String purpose = purposes.get(p.getInvoiceId());
                    if (purpose == null) {
                        StatInvoice invoice = statisticService.getInvoice(p.getInvoiceId());
                        purpose = invoice.getProduct();
                    }
                    payment.setPurpose(purpose);
                    return payment;
                }).collect(Collectors.toList());

        AtomicLong totalRefundAmnt = new AtomicLong();
        List<Refund> refundList = statisticService.getRefunds(
                report.getPartyId(),
                report.getPartyContractId(),
                fromTime,
                toTime,
                InvoicePaymentRefundStatus.succeeded(new InvoicePaymentRefundSucceeded())
        ).stream().sorted(Comparator.comparing(r -> r.getStatus().getSucceeded().getAt())).map(r -> {
            Refund refund = new Refund();
            StatPayment statPayment = statisticService.getPayment(r.getInvoiceId(), r.getPaymentId());
            refund.setId(r.getId());
            refund.setPaymentId(r.getInvoiceId() + "." + r.getPaymentId());
            //TODO captured_at only
            if (statPayment.getStatus().isSetCaptured()) {
                refund.setPaymentCapturedAt(TimeUtil.toLocalizedDateTime(statPayment.getStatus().getCaptured().getAt(), reportZoneId));
            } else {
                refund.setPaymentCapturedAt(TimeUtil.toLocalizedDateTime(statPayment.getCreatedAt(), reportZoneId));
            }
            refund.setSucceededAt(TimeUtil.toLocalizedDateTime(r.getStatus().getSucceeded().getAt(), reportZoneId));
            if (statPayment.getPayer().isSetPaymentResource()) {
                refund.setPaymentTool(statPayment.getPayer().getPaymentResource().getPaymentTool().getSetField().getFieldName());
            }
            refund.setAmount(FormatUtil.formatCurrency(r.getAmount()));
            totalRefundAmnt.addAndGet(r.getAmount());
            if (statPayment.getPayer().isSetPaymentResource()) {
                refund.setPayerEmail(statPayment.getPayer().getPaymentResource().getEmail());
            }
            refund.setShopUrl(shopUrls.get(r.getShopId()));
            String purpose = purposes.get(r.getInvoiceId());
            if (purpose == null) {
                StatInvoice invoice = statisticService.getInvoice(r.getInvoiceId());
                purpose = invoice.getProduct();
            }
            refund.setPaymentPurpose(purpose);
            return refund;
        }).collect(Collectors.toList());

        Context context = new Context();
        context.putVar("payments", paymentList);
        context.putVar("fromTime", TimeUtil.toLocalizedDate(fromTime, reportZoneId));
        context.putVar("toTime", TimeUtil.toLocalizedDate(report.getToTime().minusNanos(1).toInstant(ZoneOffset.UTC), reportZoneId));
        context.putVar("totalAmnt", FormatUtil.formatCurrency(totalAmnt.longValue()));
        context.putVar("totalPayoutAmnt", FormatUtil.formatCurrency(totalPayoutAmnt.longValue()));

        if (refundList.size() > 0) {
            context.putVar("refunds", refundList);
            context.putVar("totalRefundAmnt", FormatUtil.formatCurrency(totalRefundAmnt.longValue()));
            processTemplate(context, paymentRegistry.getInputStream(), outputStream);
        } else {
            processTemplate(context, paymentRegistryWithoutRefunds.getInputStream(), outputStream);
        }
    }

    private Instant getStatusChangedAt(StatPayment sp) {
        if (sp.getStatus().isSetCaptured()) {
            return TypeUtil.stringToInstant(sp.getStatus().getCaptured().getAt());
        } else {
            return TypeUtil.stringToInstant(sp.getStatus().getRefunded().getAt());
        }
    }

}
