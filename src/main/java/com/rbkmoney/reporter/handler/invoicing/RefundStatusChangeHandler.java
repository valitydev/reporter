package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.dao.RefundDao;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.domain.tables.pojos.RefundAdditionalInfo;
import com.rbkmoney.reporter.service.HellgateInvoicingService;
import com.rbkmoney.reporter.util.BusinessErrorUtils;
import com.rbkmoney.reporter.util.InvoicingServiceUtils;
import com.rbkmoney.reporter.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final RefundDao refundDao;

    @Override
    public List<Query> handle(MachineEvent event, InvoiceChange invoiceChange, int changeId) throws Exception {
        InvoicePaymentRefundChange invoicePaymentRefundChange = invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentRefundChange();
        InvoicePaymentRefundStatus status = invoicePaymentRefundChange.getPayload()
                .getInvoicePaymentRefundStatusChanged().getStatus();
        List<Query> refundQueries = new ArrayList<>();
        if (!status.isSetSucceeded() && !status.isSetFailed()) {
            return refundQueries;
        }
        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();

        log.info("Processing refund with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        String refundId = invoicePaymentRefundChange.getId();

        Invoice invoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(invoice, invoiceId, sequenceId, changeId);
        InvoicePayment invoicePayment = InvoicingServiceUtils.getInvoicePaymentById(
                invoice, paymentId, invoiceId, sequenceId, changeId
        );
        InvoicePaymentRefund refund = InvoicingServiceUtils.getInvoicePaymentRefundById(
                invoicePayment, refundId, invoiceId, sequenceId, changeId
        );
        InvoicePaymentRefundStatus hgRefundStatus = refund.getRefund().getStatus();
        if (!hgRefundStatus.isSetSucceeded() && !hgRefundStatus.isSetFailed()) {
            log.warn("Refund with status '{}' have incorrect status in HG (invoiceId = '{}', " +
                    "sequenceId = '{}', changeId = '{}')", status, hgRefundStatus, invoiceId,
                    sequenceId, changeId);
            return refundQueries;
        }
        Refund refundRecord = MapperUtils.createRefundRecord(refund, event, invoice, invoicePayment);
        refundQueries.add(refundDao.getSaveRefundQuery(refundRecord));
        RefundAdditionalInfo additionalInfoRecord =
                MapperUtils.createRefundAdditionalInfoRecord(refund, status, invoicePayment, event);
        refundQueries.add(refundDao.getSaveAdditionalRefundInfoQuery(additionalInfoRecord));
        log.info("Refund queries with status '{}' was created (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
        return refundQueries;
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoicePaymentChange()
                && change.getInvoicePaymentChange().getPayload().isSetInvoicePaymentRefundChange()
                && change.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange().getPayload().isSetInvoicePaymentRefundStatusChanged();
    }
}
