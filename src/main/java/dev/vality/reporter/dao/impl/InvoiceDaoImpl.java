package dev.vality.reporter.dao.impl;

import dev.vality.reporter.dao.AbstractDao;
import dev.vality.reporter.dao.InvoiceDao;
import dev.vality.reporter.domain.enums.InvoiceStatus;
import dev.vality.reporter.domain.tables.pojos.Invoice;
import dev.vality.reporter.domain.tables.pojos.InvoiceAdditionalInfo;
import dev.vality.reporter.domain.tables.records.InvoiceRecord;
import com.zaxxer.hikari.HikariDataSource;
import dev.vality.reporter.util.MapperUtils;
import org.jooq.Record1;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static dev.vality.reporter.domain.tables.Invoice.INVOICE;
import static dev.vality.reporter.domain.tables.InvoiceAdditionalInfo.INVOICE_ADDITIONAL_INFO;

@Component
public class InvoiceDaoImpl extends AbstractDao implements InvoiceDao {

    @Autowired
    public InvoiceDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long saveInvoice(Invoice invoice) {
        MapperUtils.removeNullSymbols(invoice);
        return getDslContext()
                .insertInto(INVOICE)
                .set(getDslContext().newRecord(INVOICE, invoice))
                .onConflict(INVOICE.INVOICE_ID)
                .doUpdate()
                .set(getDslContext().newRecord(INVOICE, invoice))
                .returning(INVOICE.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public InvoiceRecord getInvoice(String invoiceId) {
        return getDslContext()
                .selectFrom(INVOICE)
                .where(INVOICE.INVOICE_ID.eq(invoiceId))
                .fetchOne();
    }

    @Override
    public String getInvoicePurpose(String invoiceId) {
        final Record1<String> purpose = getDslContext()
                .select(INVOICE.PRODUCT)
                .from(INVOICE)
                .where(INVOICE.INVOICE_ID.eq(invoiceId))
                .fetchOne();
        return purpose == null ? null : purpose.value1();
    }

    @Override
    public List<Invoice> getInvoicesByState(LocalDateTime dateFrom,
                                            LocalDateTime dateTo,
                                            List<InvoiceStatus> statuses) {
        Result<InvoiceRecord> records = getDslContext()
                .selectFrom(INVOICE)
                .where(INVOICE.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(INVOICE.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(INVOICE.STATUS.in(statuses)))
                .fetch();
        return records == null || records.isEmpty() ? new ArrayList<>() : records.into(Invoice.class);
    }

    @Override
    public void saveAdditionalInvoiceInfo(InvoiceAdditionalInfo invoiceAdditionalInfo) {
        getDslContext()
                .insertInto(INVOICE_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(INVOICE_ADDITIONAL_INFO, invoiceAdditionalInfo))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(INVOICE_ADDITIONAL_INFO, invoiceAdditionalInfo))
                .execute();
    }

    @Override
    public InvoiceAdditionalInfo getInvoiceAdditionalInfo(Long extInvoiceId) {
        return getDslContext()
                .selectFrom(INVOICE_ADDITIONAL_INFO)
                .where(INVOICE_ADDITIONAL_INFO.EXT_INVOICE_ID.eq(extInvoiceId))
                .fetchOne()
                .into(InvoiceAdditionalInfo.class);
    }
}
