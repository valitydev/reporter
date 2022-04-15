package dev.vality.reporter.handler.invoicing;

import dev.vality.damsel.domain.InvoicePaymentChargebackStatus;
import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.dao.ChargebackDao;
import dev.vality.reporter.domain.tables.pojos.Chargeback;
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
public class ChargebackStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final ChargebackDao chargebackDao;

    private final FaultyEventsService faultyEventsService;

    @Override
    public void handle(KafkaEvent kafkaEvent, InvoiceChange invoiceChange, int changeId) throws Exception {
        MachineEvent event = kafkaEvent.getEvent();
        InvoicePaymentChargebackChange chargebackChange = invoiceChange.getInvoicePaymentChange()
                .getPayload().getInvoicePaymentChargebackChange();
        InvoicePaymentChargebackStatus chargebackStatus = chargebackChange
                .getPayload().getInvoicePaymentChargebackStatusChanged().getStatus();
        if (chargebackStatus.isSetPending()) {
            return;
        }

        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();

        log.debug("Processing chargeback with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", chargebackStatus, invoiceId, sequenceId, changeId);
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        String chargebackId = chargebackChange.getId();

        Invoice invoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(invoice, invoiceId, sequenceId, changeId);

        InvoicePayment invoicePayment = InvoicingServiceUtils.getInvoicePaymentById(
                invoice, paymentId, invoiceId, sequenceId, changeId
        );
        InvoicePaymentChargeback invoicePaymentChargeback = InvoicingServiceUtils.getInvoicePaymentChargebackById(
                invoicePayment, chargebackId, invoiceId, sequenceId, changeId
        );
        var chargeback = invoicePaymentChargeback.getChargeback();
        InvoicePaymentChargebackStatus hgChargebackStatus = chargeback.getStatus();
        if (hgChargebackStatus.isSetPending()) {
            log.warn("Chargeback received from kafka (topic: '{}', partition: '{}', offset: {}) " +
                            "with status '{}' have incorrect status'{}' in HG (invoiceId = '{}' " +
                            "sequenceId = '{}', changeId = '{}')", kafkaEvent.getTopic(),
                    kafkaEvent.getPartition(), kafkaEvent.getOffset(), chargebackStatus,
                    hgChargebackStatus, invoiceId, sequenceId, changeId);
            if (faultyEventsService.isFaultyEvent(kafkaEvent)) {
                return;
            }
        }

        Chargeback chargebackRecord = MapperUtils.createChargebackRecord(
                chargeback, invoicePayment, invoice, event
        );
        chargebackDao.saveChargeback(chargebackRecord);
        log.info("Chargeback with status '{}' was processed (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", chargebackStatus, invoiceId, sequenceId, changeId);
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoicePaymentChange()
                && change.getInvoicePaymentChange().getPayload().isSetInvoicePaymentChargebackChange()
                && change.getInvoicePaymentChange().getPayload().getInvoicePaymentChargebackChange().getPayload()
                .isSetInvoicePaymentChargebackStatusChanged();
    }
}
