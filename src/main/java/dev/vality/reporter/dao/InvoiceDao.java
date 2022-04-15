package dev.vality.reporter.dao;

import dev.vality.reporter.domain.enums.InvoiceStatus;
import dev.vality.reporter.domain.tables.pojos.Invoice;
import dev.vality.reporter.domain.tables.pojos.InvoiceAdditionalInfo;
import dev.vality.reporter.domain.tables.records.InvoiceRecord;

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
