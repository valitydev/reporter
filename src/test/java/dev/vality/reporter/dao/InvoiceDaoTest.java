package dev.vality.reporter.dao;

import dev.vality.reporter.config.PostgresqlSpringBootITest;
import dev.vality.reporter.domain.enums.InvoiceStatus;
import dev.vality.reporter.domain.tables.pojos.Invoice;
import dev.vality.reporter.domain.tables.records.InvoiceRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static dev.vality.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    public void getPurposeTest() {
        Invoice invoice = random(Invoice.class);
        String product = "TestProduct";
        invoice.setProduct(product);
        invoiceDao.saveInvoice(invoice);
        String invoicePurpose = invoiceDao.getInvoicePurpose(invoice.getInvoiceId());
        assertNotNull(invoicePurpose);
        assertEquals(product, invoicePurpose);
    }
}
