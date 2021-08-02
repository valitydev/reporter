package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.PostgresqlSpringBootITest;
import com.rbkmoney.reporter.domain.enums.InvoiceStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.records.InvoiceRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.rbkmoney.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class InvoiceDaoTest {

    @Autowired
    private InvoiceDao invoiceDao;

    @Test
    public void saveAndGetInvoiceTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);
        Invoice invoice = random(Invoice.class);
        invoice.setPartyId(partyId);
        invoice.setShopId(shopId + "\u0000" + "x");
        invoice.setStatus(InvoiceStatus.paid);
        invoiceDao.saveInvoice(invoice);
        InvoiceRecord invoiceRecordOne = invoiceDao.getInvoice(invoice.getInvoiceId());
        assertEquals(invoice, invoiceRecordOne.into(Invoice.class));
    }
}
