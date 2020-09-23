package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.config.AbstractDaoConfig;
import com.rbkmoney.reporter.config.AbstractHandlerConfig;
import com.rbkmoney.reporter.dao.impl.InvoiceDaoImpl;
import com.rbkmoney.reporter.dao.impl.PaymentDaoImpl;
import com.rbkmoney.reporter.dao.impl.RefundDaoImpl;
import com.rbkmoney.reporter.domain.enums.InvoiceStatus;
import com.rbkmoney.reporter.service.HellgateInvoicingService;
import org.jooq.Query;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static com.rbkmoney.reporter.data.InvoicingData.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@ContextConfiguration(
        classes = {
                InvoiceStatusChangeHandler.class,
                PaymentStatusChangeHandler.class,
                RefundStatusChangeHandler.class,
                HellgateInvoicingService.class,
                InvoiceDaoImpl.class,
                PaymentDaoImpl.class,
                RefundDaoImpl.class
        },
        initializers = AbstractHandlerConfig.Initializer.class
)
public class InvoicingHandlerTest extends AbstractDaoConfig {

    @Autowired
    private InvoiceStatusChangeHandler invoiceHandler;

    @Autowired
    private PaymentStatusChangeHandler paymentHandler;

    @Autowired
    private RefundStatusChangeHandler refundHandler;

    @MockBean
    private HellgateInvoicingService hgInvoicingService;

    private static final String INVOICE_ID = "inv-1";
    private static final String INVOICE_ID_2 = "inv-2";
    private static final String PAYMENT_ID = "pay-1";
    private static final String REFUND_ID = "ref-1";
    private static final String ADJUSTMENT_ID = "adj-1";

    private MachineEvent machineEventOne = createMachineEvent(INVOICE_ID);
    private MachineEvent machineEventTwo = createMachineEvent(INVOICE_ID_2);

    @Before
    public void init() throws Exception {
        when(hgInvoicingService.getInvoice(INVOICE_ID, 1L))
                .thenReturn(createHgInvoice(INVOICE_ID, PAYMENT_ID, REFUND_ID, ADJUSTMENT_ID));
        when(hgInvoicingService.getInvoice(INVOICE_ID_2, 1L))
                .thenReturn(createHgInvoice(INVOICE_ID_2, PAYMENT_ID, REFUND_ID, ADJUSTMENT_ID));
    }

    @Test
    public void invoiceHandlerTest() throws Exception {
        List<Query> handle = invoiceHandler.handle(
                machineEventOne,
                createInvoiceStatusChange(InvoiceStatus.paid),
                1
        );
        assertEquals(2,handle.size());
    }

    @Test
    public void paymentHandlerTest() throws Exception {
        List<Query> handle = paymentHandler.handle(
                machineEventOne,
                createCapturePaymentStatusChange(),
                1
        );
        assertEquals(2,handle.size());
    }

    @Test
    public void refundHandlerTest() throws Exception {
        List<Query> handle = refundHandler.handle(
                machineEventOne,
                createSucceededRefundStatusChange(),
                1
        );
        assertEquals(2,handle.size());
    }

}
