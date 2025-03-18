package dev.vality.reporter.handler.invoicing;

import dev.vality.damsel.domain.InvoicePaymentStatus;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.dao.PaymentDao;
import dev.vality.reporter.domain.tables.pojos.Payment;
import dev.vality.reporter.model.KafkaEvent;
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
public class PaymentCashChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;
    private final PaymentDao paymentDao;

    @Override
    public void handle(KafkaEvent kafkaEvent, InvoiceChange invoiceChange, int changeId) throws Exception {
        MachineEvent event = kafkaEvent.getEvent();
        var invoicePaymentCashChanged =
                invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentCashChanged();
        var newCash = invoicePaymentCashChanged.getNewCash();
        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();
        log.debug("Processing payment cash changed with newCash '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", newCash, invoiceId, sequenceId, changeId);
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        var hgInvoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(hgInvoice, invoiceId, sequenceId, changeId);
        var payment = InvoicingServiceUtils.getInvoicePaymentById(
                hgInvoice, paymentId, invoiceId, sequenceId, changeId
        );
        InvoicePaymentStatus hgPaymentStatus = payment.getPayment().getStatus();
        if (!hgPaymentStatus.isSetCaptured() && !hgPaymentStatus.isSetCancelled()
                && !hgPaymentStatus.isSetFailed()) {
            return;
        }
        Payment paymentRecord = MapperUtils.createPaymentRecord(event, hgInvoice, payment);
        paymentRecord.setAmount(newCash.getAmount());
        Long extPaymentId = paymentDao.savePayment(paymentRecord);
        log.info("Payment amount '{}' was updated (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}', extPaymentId = '{}')", newCash, invoiceId, sequenceId, changeId, extPaymentId);
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoicePaymentChange()
                && change.getInvoicePaymentChange().getPayload().isSetInvoicePaymentCashChanged();
    }

}
