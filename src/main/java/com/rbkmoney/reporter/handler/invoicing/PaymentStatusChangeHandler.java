package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.domain.InvoicePaymentStatus;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentStatusChanged;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.PaymentAdditionalInfo;
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
public class PaymentStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final PaymentDao paymentDao;

    @Override
    public List<Query> handle(MachineEvent event,
                              InvoiceChange invoiceChange,
                              int changeId) throws Exception{
        InvoicePaymentStatusChanged invoicePaymentStatusChanged =
                invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged();
        InvoicePaymentStatus status = invoicePaymentStatusChanged.getStatus();
        List<Query> paymentQueries = new ArrayList<>();
        if (!status.isSetCaptured() && !status.isSetCancelled() && !status.isSetFailed()) {
            return paymentQueries;
        }
        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();
        log.info("Processing payment with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
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
            log.warn("Payment with status '{}' have incorrect status in HG '{}' (invoiceId = '{}', " +
                    "sequenceId = '{}', changeId = '{}')", status, hgPaymentStatus, invoiceId,
                    sequenceId, changeId);
            return paymentQueries;
        }

        Payment paymentRecord = MapperUtils.createPaymentRecord(event, hgInvoice, payment);
        paymentQueries.add(paymentDao.getSavePaymentQuery(paymentRecord));
        PaymentAdditionalInfo paymentAdditionalInfoRecord = MapperUtils.createPaymentAdditionalInfoRecord(
                event, payment, invoicePaymentStatusChanged, changeId
        );
        paymentQueries.add(paymentDao.getSaveAdditionalPaymentInfoQuery(paymentAdditionalInfoRecord));
        log.info("Payment queries with status '{}' was created (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", status, invoiceId, sequenceId, changeId);
        return paymentQueries;
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoicePaymentChange()
                && change.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStatusChanged();
    }

}
