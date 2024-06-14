package dev.vality.reporter.handler.invoicing;

import dev.vality.damsel.domain.InvoicePaymentStatus;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentStatusChanged;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.dao.PaymentDao;
import dev.vality.reporter.domain.tables.pojos.Payment;
import dev.vality.reporter.domain.tables.pojos.PaymentAdditionalInfo;
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
public class PaymentStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final PaymentDao paymentDao;

    private final FaultyEventsService faultyEventsService;

    @Override
    public void handle(KafkaEvent kafkaEvent, InvoiceChange invoiceChange, int changeId) throws Exception {
        MachineEvent event = kafkaEvent.getEvent();
        InvoicePaymentStatusChanged invoicePaymentStatusChanged =
                invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged();
        InvoicePaymentStatus status = invoicePaymentStatusChanged.getStatus();
        if (!status.isSetCaptured() && !status.isSetCancelled() && !status.isSetFailed()) {
            return;
        }
        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();
        log.debug("Processing payment with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        var hgInvoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(hgInvoice, invoiceId, sequenceId, changeId);

        var payment = InvoicingServiceUtils.getInvoicePaymentById(
                hgInvoice, paymentId, invoiceId, sequenceId, changeId
        );

        InvoicePaymentStatus hgPaymentStatus = payment.getPayment().getStatus();
        if (!hgPaymentStatus.isSetCaptured() && !hgPaymentStatus.isSetCancelled()
                && !hgPaymentStatus.isSetFailed()) {
            log.warn("Payment received from kafka (topic: '{}', partition: '{}', offset: {}) " +
                            "with status '{}' have incorrect status in HG '{}' (invoiceId = '{}', " +
                            "sequenceId = '{}', changeId = '{}')", kafkaEvent.getTopic(),
                    kafkaEvent.getPartition(), kafkaEvent.getOffset(), status, hgPaymentStatus, invoiceId,
                    sequenceId, changeId);
            if (faultyEventsService.isFaultyEvent(kafkaEvent)) {
                return;
            }
        }

        Payment paymentRecord = MapperUtils.createPaymentRecord(event, hgInvoice, payment, status);
        Long extPaymentId = paymentDao.savePayment(paymentRecord);
        PaymentAdditionalInfo paymentAdditionalInfoRecord = MapperUtils.createPaymentAdditionalInfoRecord(
                event, payment, invoicePaymentStatusChanged, extPaymentId, changeId
        );
        paymentDao.saveAdditionalPaymentInfo(paymentAdditionalInfoRecord);
        log.info("Payment with status '{}' was saved (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoicePaymentChange()
                && change.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStatusChanged();
    }

}
