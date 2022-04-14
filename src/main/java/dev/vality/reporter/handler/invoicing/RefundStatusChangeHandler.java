package dev.vality.reporter.handler.invoicing;

import dev.vality.damsel.domain.InvoicePaymentRefundStatus;
import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.dao.RefundDao;
import dev.vality.reporter.domain.tables.pojos.Refund;
import dev.vality.reporter.domain.tables.pojos.RefundAdditionalInfo;
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
public class RefundStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final RefundDao refundDao;

    private final FaultyEventsService faultyEventsService;

    @Override
    public void handle(KafkaEvent kafkaEvent, InvoiceChange invoiceChange, int changeId) throws Exception {
        MachineEvent event = kafkaEvent.getEvent();
        InvoicePaymentRefundChange invoicePaymentRefundChange = invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentRefundChange();
        InvoicePaymentRefundStatus status = invoicePaymentRefundChange.getPayload()
                .getInvoicePaymentRefundStatusChanged().getStatus();
        if (!status.isSetSucceeded() && !status.isSetFailed()) {
            return;
        }
        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();

        log.debug("Processing refund with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
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
            log.warn("Refund received from kafka (topic: '{}', partition: '{}', offset: {}) " +
                            "with status '{}' have incorrect status in HG (invoiceId = '{}', " +
                            "sequenceId = '{}', changeId = '{}')", kafkaEvent.getTopic(),
                    kafkaEvent.getPartition(), kafkaEvent.getOffset(), status, hgRefundStatus, invoiceId,
                    sequenceId, changeId);
            if (faultyEventsService.isFaultyEvent(kafkaEvent)) {
                return;
            }
        }
        Refund refundRecord = MapperUtils.createRefundRecord(refund, event, invoice, invoicePayment);
        Long extRefundId = refundDao.saveRefund(refundRecord);
        RefundAdditionalInfo additionalInfoRecord =
                MapperUtils.createRefundAdditionalInfoRecord(refund, status, extRefundId);
        refundDao.saveAdditionalRefundInfo(additionalInfoRecord);
        log.info("Refund with status '{}' was saved (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoicePaymentChange()
                && change.getInvoicePaymentChange().getPayload().isSetInvoicePaymentRefundChange()
                && change.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange().getPayload()
                .isSetInvoicePaymentRefundStatusChanged();
    }
}
