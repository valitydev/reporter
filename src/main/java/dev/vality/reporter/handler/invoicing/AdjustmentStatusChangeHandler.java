package dev.vality.reporter.handler.invoicing;

import dev.vality.damsel.domain.InvoicePaymentAdjustment;
import dev.vality.damsel.domain.InvoicePaymentAdjustmentStatus;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.dao.AdjustmentDao;
import dev.vality.reporter.domain.tables.pojos.Adjustment;
import dev.vality.reporter.model.KafkaEvent;
import dev.vality.reporter.service.FaultyEventsService;
import dev.vality.reporter.service.HellgateInvoicingService;
import dev.vality.reporter.util.BusinessErrorUtils;
import dev.vality.reporter.util.InvoicingServiceUtils;
import dev.vality.reporter.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdjustmentStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final AdjustmentDao adjustmentDao;

    private final FaultyEventsService faultyEventsService;

    @Override
    public void handle(KafkaEvent kafkaEvent, InvoiceChange invoiceChange, int changeId) throws Exception {
        MachineEvent event = kafkaEvent.getEvent();
        InvoicePaymentAdjustmentChange adjustmentChange = invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentAdjustmentChange();
        InvoicePaymentAdjustmentStatus status = adjustmentChange.getPayload()
                .getInvoicePaymentAdjustmentStatusChanged().getStatus();
        if (!status.isSetCaptured() && !status.isSetCancelled()) {
            return;
        }

        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();

        log.debug("Processing adjustment with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
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
            log.warn("Adjustment received from kafka (topic: '{}', partition: '{}', offset: {}) " +
                            "with status '{}' have incorrect status'{}' in HG (invoiceId = '{}' " +
                            "sequenceId = '{}', changeId = '{}')", kafkaEvent.getTopic(), kafkaEvent.getPartition(),
                    kafkaEvent.getOffset(), status, hgAdjustmentStatus, invoiceId, sequenceId, changeId);
            if (faultyEventsService.isFaultyEvent(kafkaEvent)) {
                return;
            }
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
                && change.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange().getPayload()
                .isSetInvoicePaymentAdjustmentStatusChanged();
    }
}
