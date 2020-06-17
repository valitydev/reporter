package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.dao.InvoiceDao;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.pojos.InvoiceAdditionalInfo;
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

    @Override
    @Transactional
    public void handle(MachineEvent event, InvoiceChange change, int changeId) throws Exception {
        var invoiceStatus = change.getInvoiceStatusChanged().getStatus();
        if (!invoiceStatus.isSetPaid() && !invoiceStatus.isSetCancelled()) {
            return;
        }

        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();
        log.info("Start irocessing invoice with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", invoiceStatus, invoiceId, sequenceId, changeId);

        var hgInvoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(hgInvoice, invoiceId, sequenceId, changeId);

        Invoice invoiceRecord = MapperUtils.createInvoiceRecord(hgInvoice, event);
        Long extInvoiceId = invoiceDao.saveInvoice(invoiceRecord);

        InvoiceAdditionalInfo invoiceAdditionalInfo = MapperUtils.createInvoiceAdditionalInfoRecord(
                hgInvoice, extInvoiceId
        );
        invoiceDao.saveAdditionalInvoiceInfo(invoiceAdditionalInfo);
        log.info("Processing invoice with status '{}' completed (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", invoiceStatus, invoiceId, sequenceId, changeId);
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoiceStatusChanged();
    }
}
