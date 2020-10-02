package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.domain.InvoicePaymentAdjustment;
import com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.dao.AdjustmentDao;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.service.HellgateInvoicingService;
import com.rbkmoney.reporter.util.BusinessErrorUtils;
import com.rbkmoney.reporter.util.InvoicingServiceUtils;
import com.rbkmoney.reporter.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdjustmentStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final AdjustmentDao adjustmentDao;

    @Override
    public void handle(MachineEvent event, InvoiceChange invoiceChange, int changeId) throws Exception {
        InvoicePaymentAdjustmentChange adjustmentChange = invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentAdjustmentChange();
        InvoicePaymentAdjustmentStatus status = adjustmentChange.getPayload()
                .getInvoicePaymentAdjustmentStatusChanged().getStatus();
        if (!status.isSetCaptured() && !status.isSetCancelled()) {
            return;
        }

        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();

        log.info("Processing adjustment with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        String adjustmentId = adjustmentChange.getId();

        Invoice invoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(invoice, invoiceId, sequenceId, changeId);

        InvoicePayment invoicePayment = InvoicingServiceUtils.getInvoicePaymentById(
                invoice, paymentId, invoiceId, sequenceId, changeId
        );
        InvoicePaymentAdjustment paymentAdjustment = InvoicingServiceUtils.getInvoicePaymentAdjustmentById(
                invoicePayment, adjustmentId, invoiceId, sequenceId, changeId
        );
        InvoicePaymentAdjustmentStatus hgAdjustmentStatus = paymentAdjustment.getStatus();
        if (hgAdjustmentStatus.isSetPending() || hgAdjustmentStatus.isSetProcessed()) {
            log.warn("Adjustment with status '{}' have incorrect status'{}' in HG (invoiceId = '{}', " +
                    "sequenceId = '{}', changeId = '{}')", status, hgAdjustmentStatus, invoiceId,
                    sequenceId, changeId);
        }

        Adjustment adjustmentRecord = MapperUtils.createAdjustmentRecord(
                paymentAdjustment, invoicePayment, invoice, event
        );
        adjustmentDao.saveAdjustment(adjustmentRecord);
        log.info("Adjustment with status '{}' was processed (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoicePaymentChange()
                && change.getInvoicePaymentChange().getPayload().isSetInvoicePaymentAdjustmentChange()
                && change.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange().getPayload().isSetInvoicePaymentAdjustmentStatusChanged();
    }
}
