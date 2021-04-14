package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.domain.InvoiceStatus;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.dao.InvoiceDao;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.pojos.InvoiceAdditionalInfo;
import com.rbkmoney.reporter.model.KafkaEvent;
import com.rbkmoney.reporter.service.FaultyEventsService;
import com.rbkmoney.reporter.service.HellgateInvoicingService;
import com.rbkmoney.reporter.util.BusinessErrorUtils;
import com.rbkmoney.reporter.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final InvoiceDao invoiceDao;

    private final FaultyEventsService faultyEventsService;

    @Override
    @Transactional
    public void handle(KafkaEvent kafkaEvent, InvoiceChange change, int changeId) throws Exception {
        MachineEvent event = kafkaEvent.getEvent();
        var invoiceStatus = change.getInvoiceStatusChanged().getStatus();
        if (!invoiceStatus.isSetPaid() && !invoiceStatus.isSetCancelled()) {
            return;
        }

        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();
        log.debug("Start processing invoice with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", invoiceStatus, invoiceId, sequenceId, changeId);

        var hgInvoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(hgInvoice, invoiceId, sequenceId, changeId);
        InvoiceStatus hgInvoiceStatus = hgInvoice.getInvoice().getStatus();
        if (!hgInvoiceStatus.isSetPaid() && !hgInvoiceStatus.isSetCancelled()) {
            log.warn("Invoice received from kafka (topic: '{}', partition: '{}', offset: {}) " +
                            "with a status '{}' have incorrect status '{}' in HG (invoiceId = '{}', " +
                            "sequenceId = '{}', changeId = '{}')", kafkaEvent.getTopic(),
                    kafkaEvent.getPartition(), kafkaEvent.getOffset(), invoiceStatus, hgInvoiceStatus,
                    invoiceId, sequenceId, changeId);
            if (faultyEventsService.isFaultyEvent(kafkaEvent)) {
                return;
            }
        }
        Long extInvoiceId = processInvoiceData(hgInvoice, event);

        InvoiceAdditionalInfo invoiceAdditionalInfo = MapperUtils.createInvoiceAdditionalInfoRecord(
                hgInvoice, extInvoiceId
        );
        invoiceDao.saveAdditionalInvoiceInfo(invoiceAdditionalInfo);
        log.info("Processing invoice with status '{}' completed (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", invoiceStatus, invoiceId, sequenceId, changeId);
    }

    private Long processInvoiceData(com.rbkmoney.damsel.payment_processing.Invoice hgInvoice,
                                    MachineEvent event) {
        Invoice invoiceRecord = null;
        try {
            invoiceRecord = MapperUtils.createInvoiceRecord(hgInvoice, event);
            return invoiceDao.saveInvoice(invoiceRecord);
        } catch (Exception ex) {
            log.error("Received an error when service processed invoice data (invoiceRecord: {}, " +
                    "hgInvoice: {}, event: {})", invoiceRecord == null ? "empty" : invoiceRecord,
                    hgInvoice, event, ex);
            throw ex;
        }
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoiceStatusChanged();
    }
}
