package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.mapper.RecordRowMapper;
import com.rbkmoney.reporter.dao.InvoiceDao;
import com.rbkmoney.reporter.domain.enums.InvoiceStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.pojos.InvoiceAdditionalInfo;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.Invoice.INVOICE;
import static com.rbkmoney.reporter.domain.tables.InvoiceAdditionalInfo.INVOICE_ADDITIONAL_INFO;

@Component
public class InvoiceDaoImpl extends AbstractGenericDao implements InvoiceDao {

    private final RowMapper<Invoice> invoiceRowMapper;
    private final RowMapper<InvoiceAdditionalInfo> invoiceAdditionalInfoRowMapper;

    @Autowired
    public InvoiceDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        invoiceRowMapper = new RecordRowMapper<>(INVOICE, Invoice.class);
        invoiceAdditionalInfoRowMapper = new RecordRowMapper<>(INVOICE_ADDITIONAL_INFO, InvoiceAdditionalInfo.class);
    }

    @Override
    public Long saveInvoice(Invoice invoice) {
        Query query = getDslContext()
                .insertInto(INVOICE)
                .set(getDslContext().newRecord(INVOICE, invoice))
                .onConflict(INVOICE.INVOICE_ID)
                .doUpdate()
                .set(getDslContext().newRecord(INVOICE, invoice))
                .returning(INVOICE.ID);
        return fetchOne(query, Long.class);
    }

    @Override
    public Invoice getInvoice(String invoiceId) {
        Query query = getDslContext()
                .selectFrom(INVOICE)
                .where(INVOICE.INVOICE_ID.eq(invoiceId));
        return fetchOne(query, invoiceRowMapper);
    }

    @Override
    public List<Invoice> getInvoicesByState(LocalDateTime dateFrom, LocalDateTime dateTo, List<InvoiceStatus> statuses) {
        Query query = getDslContext()
                .selectFrom(INVOICE)
                .where(INVOICE.CREATED_AT.greaterThan(dateFrom)
                        .and(INVOICE.CREATED_AT.lessThan(dateTo))
                        .and(INVOICE.STATUS.in(statuses)));
        return fetch(query, invoiceRowMapper);
    }

    @Override
    public void saveAdditionalInvoiceInfo(InvoiceAdditionalInfo invoiceAdditionalInfo) {
        Query query = getDslContext()
                .insertInto(INVOICE_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(INVOICE_ADDITIONAL_INFO, invoiceAdditionalInfo))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(INVOICE_ADDITIONAL_INFO, invoiceAdditionalInfo));
        executeOne(query);
    }

    @Override
    public InvoiceAdditionalInfo getInvoiceAdditionalInfo(Long extInvoiceId) {
        Query query = getDslContext()
                .selectFrom(INVOICE_ADDITIONAL_INFO)
                .where(INVOICE_ADDITIONAL_INFO.EXT_INVOICE_ID.eq(extInvoiceId));
        return fetchOne(query, invoiceAdditionalInfoRowMapper);
    }
}
