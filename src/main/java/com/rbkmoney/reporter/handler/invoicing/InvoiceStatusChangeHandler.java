package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.domain.InvoiceStatus;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.dao.InvoiceDao;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.pojos.InvoiceAdditionalInfo;
import org.jooq.Query;
import com.rbkmoney.reporter.service.HellgateInvoicingService;
import com.rbkmoney.reporter.util.BusinessErrorUtils;
import com.rbkmoney.reporter.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceStatusChangeHandler implements InvoicingEventHandler {

    private final HellgateInvoicingService hgInvoicingService;

    private final InvoiceDao invoiceDao;

    @Override
    @Transactional
    public List<Query> handle(MachineEvent event,
                              InvoiceChange change,
                              int changeId) throws Exception {
        var invoiceStatus = change.getInvoiceStatusChanged().getStatus();
        List<Query> invoiceQueries = new ArrayList<>();
        if (!invoiceStatus.isSetPaid() && !invoiceStatus.isSetCancelled()) {
            return invoiceQueries;
        }

        String invoiceId = event.getSourceId();
        long sequenceId = event.getEventId();
        log.info("Start processing invoice with status '{}' (invoiceId = '{}', sequenceId = '{}', " +
                "changeId = '{}')", invoiceStatus, invoiceId, sequenceId, changeId);

        var hgInvoice = hgInvoicingService.getInvoice(invoiceId, sequenceId);
        BusinessErrorUtils.checkInvoiceCorrectness(hgInvoice, invoiceId, sequenceId, changeId);
        InvoiceStatus hgInvoiceStatus = hgInvoice.getInvoice().getStatus();
        if (!hgInvoiceStatus.isSetPaid() && !hgInvoiceStatus.isSetCancelled()) {
            log.warn("Invoice with a status '{}' have incorrect status '{}' in HG (invoiceId = '{}', " +
                    "sequenceId = '{}', changeId = '{}')", invoiceStatus, hgInvoiceStatus, invoiceId,
                    sequenceId, changeId);
        }
        Invoice invoiceRecord = MapperUtils.createInvoiceRecord(hgInvoice, event);
        invoiceQueries.add(invoiceDao.getSaveInvoiceQuery(invoiceRecord));

        InvoiceAdditionalInfo invoiceAdditionalInfo = MapperUtils.createInvoiceAdditionalInfoRecord(
                hgInvoice, event
        );
        invoiceQueries.add(invoiceDao.getSaveAdditionalInvoiceInfoQuery(invoiceAdditionalInfo));
        log.info("Processing queries for invoice with status '{}' completed (invoiceId = '{}', " +
                "sequenceId = '{}', changeId = '{}')", invoiceStatus, invoiceId, sequenceId, changeId);
        return invoiceQueries;
    }

    @Override
    public boolean isAccept(InvoiceChange change) {
        return change.isSetInvoiceStatusChanged();
    }
}
