package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.InvoiceStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.pojos.InvoiceAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.InvoiceRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceDao {

    Long saveInvoice(Invoice invoice);

    InvoiceRecord getInvoice(String invoiceId);

    String getInvoicePurpose(String invoiceId);

    List<Invoice> getInvoicesByState(LocalDateTime dateFrom, LocalDateTime dateTo, List<InvoiceStatus> statuses);

    void saveAdditionalInvoiceInfo(InvoiceAdditionalInfo invoiceAdditionalInfo);

    InvoiceAdditionalInfo getInvoiceAdditionalInfo(Long extInvoiceId);

}
