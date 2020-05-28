package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.InvoiceStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.pojos.InvoiceAdditionalInfo;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceDao {

    Long saveInvoice(Invoice invoice);

    Invoice getInvoice(String invoiceId);

    List<Invoice> getInvoicesByState(LocalDateTime dateFrom, LocalDateTime dateTo, List<InvoiceStatus> statuses);

    void saveAdditionalInvoiceInfo(InvoiceAdditionalInfo invoiceAdditionalInfo);

    InvoiceAdditionalInfo getInvoiceAdditionalInfo(Long extInvoiceId);

}
