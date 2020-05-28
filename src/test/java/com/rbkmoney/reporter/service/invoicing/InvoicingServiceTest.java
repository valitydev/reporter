package com.rbkmoney.reporter.service.invoicing;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.EventRange;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.damsel.payment_processing.UserInfo;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.config.AbstractInvoicingServiceConfig;
import com.rbkmoney.reporter.dao.AdjustmentDao;
import com.rbkmoney.reporter.dao.InvoiceDao;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.dao.RefundDao;
import com.rbkmoney.reporter.data.InvoicingData;
import com.rbkmoney.reporter.domain.enums.AdjustmentStatus;
import com.rbkmoney.reporter.domain.enums.InvoiceStatus;
import com.rbkmoney.reporter.domain.enums.RefundStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Adjustment;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.Refund;
import com.rbkmoney.reporter.handler.invoicing.InvoiceStatusChangeHandler;
import com.rbkmoney.reporter.service.impl.InvoicingService;
import com.rbkmoney.sink.common.parser.Parser;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.rbkmoney.reporter.data.InvoicingData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class InvoicingServiceTest extends AbstractInvoicingServiceConfig {

    @Autowired
    private InvoicingService invoicingService;

    @Autowired
    private InvoiceDao invoiceDao;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private AdjustmentDao adjustmentDao;

    @Autowired
    private InvoiceStatusChangeHandler invoiceStatusChangeHandler;

    @MockBean
    private Parser<MachineEvent, EventPayload> paymentMachineEventParser;

    @MockBean
    private InvoicingSrv.Iface hgInvoicingService;

    private static final String INVOICE_ID = "inv-1";
    private static final String INVOICE_ID_2 = "inv-2";
    private static final String PAYMENT_ID = "pay-1";
    private static final String REFUND_ID = "ref-1";
    private static final String ADJUSTMENT_ID = "adj-1";

    private MachineEvent machineEventOne = createMachineEvent(INVOICE_ID);
    private MachineEvent machineEventTwo = createMachineEvent(INVOICE_ID_2);

    @Before
    public void init() throws TException {
        UserInfo userInfo = InvoiceStatusChangeHandler.USER_INFO;
        EventRange eventRange = invoiceStatusChangeHandler.getEventRange(1);
        when(hgInvoicingService.get(userInfo, INVOICE_ID, eventRange))
                .thenReturn(createHgInvoice(INVOICE_ID, PAYMENT_ID, REFUND_ID, ADJUSTMENT_ID));
        when(hgInvoicingService.get(userInfo, INVOICE_ID_2, eventRange))
                .thenReturn(createHgInvoice(INVOICE_ID_2, PAYMENT_ID, REFUND_ID, ADJUSTMENT_ID));
    }

    @Test
    public void addNewInvoiceTest() throws Exception {
        List<InvoicingData.InvoiceChangeStatusInfo> firstStatusInfoList = new ArrayList<>();
        firstStatusInfoList.add(new InvoicingData.InvoiceChangeStatusInfo(
                1, InvoiceStatus.paid));
        firstStatusInfoList.add(new InvoicingData.InvoiceChangeStatusInfo(
                1, InvoiceStatus.unpaid));

        List<InvoicingData.InvoiceChangeStatusInfo> secondStatusInfoList = new ArrayList<>();
        secondStatusInfoList.add(new InvoicingData.InvoiceChangeStatusInfo(
                1, InvoiceStatus.unpaid));

        when(paymentMachineEventParser.parse(machineEventOne))
                .thenReturn(createTestInvoiceEventPayload(firstStatusInfoList));
        when(paymentMachineEventParser.parse(machineEventTwo))
                .thenReturn(createTestInvoiceEventPayload(secondStatusInfoList));

        invoicingService.handleEvents(Arrays.asList(machineEventOne));
        invoicingService.handleEvents(Arrays.asList(machineEventTwo));
        List<Invoice> invoices = invoiceDao.getInvoicesByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(InvoiceStatus.paid, InvoiceStatus.unpaid, InvoiceStatus.cancelled, InvoiceStatus.fulfilled)
        );
        assertEquals("Received count of invoices is not equal to expected", 1, invoices.size());
        Invoice invoice = invoices.get(0);
        assertTrue("Received invoice id is not equal to expected", INVOICE_ID.equals(invoice.getInvoiceId()));
        assertTrue("Received invoice status is not equal to expected", invoice.getStatus() == InvoiceStatus.paid);
        assertTrue("Received invoice currency is not equal to expected", "RUR".equals(invoice.getCurrencyCode()));
        assertTrue("Received invoice amount is not equal to expected", invoice.getAmount() == 1000L);
    }

    @Test
    public void addNewPaymentTest() throws Exception {
        List<InvoicingData.PaymentChangeStatusInfo> firstStatusInfoList = new ArrayList<>();
        InvoicePaymentStatus captureStatus = new InvoicePaymentStatus();
        captureStatus.setCaptured(new InvoicePaymentCaptured());
        firstStatusInfoList.add(new InvoicingData.PaymentChangeStatusInfo(PAYMENT_ID, captureStatus));

        InvoicePaymentStatus pendingStatus = new InvoicePaymentStatus();
        pendingStatus.setPending(new InvoicePaymentPending());
        firstStatusInfoList.add(new InvoicingData.PaymentChangeStatusInfo("2", pendingStatus));

        List<InvoicingData.PaymentChangeStatusInfo> secondStatusInfoList = new ArrayList<>();
        InvoicePaymentStatus pendingStatusTwo = new InvoicePaymentStatus();
        pendingStatusTwo.setPending(new InvoicePaymentPending());
        secondStatusInfoList.add(new InvoicingData.PaymentChangeStatusInfo(PAYMENT_ID, pendingStatusTwo));

        when(paymentMachineEventParser.parse(machineEventOne))
                .thenReturn(createTestPaymentEventPayload(firstStatusInfoList));
        when(paymentMachineEventParser.parse(machineEventTwo))
                .thenReturn(createTestPaymentEventPayload(secondStatusInfoList));

        invoicingService.handleEvents(Arrays.asList(machineEventOne));
        invoicingService.handleEvents(Arrays.asList(machineEventTwo));
        List<Payment> payments = paymentDao.getPaymentsByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus.captured,
                        com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus.cancelled,
                        com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus.failed)
        );
        assertEquals("Received count of invoices is not equal to expected", 1, payments.size());
        Payment payment = payments.get(0);
        assertTrue("Received payment invoice id is not equal to expected", INVOICE_ID.equals(payment.getInvoiceId()));
        assertTrue("Received payment id is not equal to expected", PAYMENT_ID.equals(payment.getPaymentId()));
        assertTrue("Received payment status is not equal to expected",
                payment.getStatus() == com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus.captured);
        assertTrue("Received payment currency is not equal to expected", "RUR".equals(payment.getCurrencyCode()));
        assertTrue("Received payment amount is not equal to expected", payment.getAmount() == 1000L);
    }

    @Test
    public void addNewRefundTest() throws Exception {
        List<InvoicingData.RefundChangeStatusInfo> firstStatusInfoList = new ArrayList<>();
        var captureStatus = new com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus();
        captureStatus.setSucceeded(new InvoicePaymentRefundSucceeded());
        firstStatusInfoList.add(new InvoicingData.RefundChangeStatusInfo(PAYMENT_ID, REFUND_ID, captureStatus));
        var pendingStatus = new com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus();
        pendingStatus.setPending(new InvoicePaymentRefundPending());
        firstStatusInfoList.add(new InvoicingData.RefundChangeStatusInfo(PAYMENT_ID, "2", pendingStatus));

        List<InvoicingData.RefundChangeStatusInfo> secondStatusInfoList = new ArrayList<>();
        var pendingStatusTwo = new com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus();
        pendingStatusTwo.setPending(new InvoicePaymentRefundPending());
        secondStatusInfoList.add(new InvoicingData.RefundChangeStatusInfo(PAYMENT_ID, REFUND_ID, pendingStatusTwo));

        when(paymentMachineEventParser.parse(machineEventOne))
                .thenReturn(createTestRefundEventPayload(firstStatusInfoList));
        when(paymentMachineEventParser.parse(machineEventTwo))
                .thenReturn(createTestRefundEventPayload(secondStatusInfoList));

        invoicingService.handleEvents(Arrays.asList(machineEventOne));
        invoicingService.handleEvents(Arrays.asList(machineEventTwo));

        List<Refund> refunds = refundDao.getRefundsByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(RefundStatus.failed, RefundStatus.succeeded)
        );
        assertEquals("Received count of refunds is not equal to expected", 1, refunds.size());

        Refund refund = refunds.get(0);
        assertTrue("Received refund invoice id is not equal to expected", INVOICE_ID.equals(refund.getInvoiceId()));
        assertTrue("Received refund payment id is not equal to expected", PAYMENT_ID.equals(refund.getPaymentId()));
        assertTrue("Received refund id is not equal to expected", REFUND_ID.equals(refund.getRefundId()));
        assertTrue("Received refund status is not equal to expected", refund.getStatus() == RefundStatus.succeeded);
        assertTrue("Received refund currency is not equal to expected", "RUR".equals(refund.getCurrencyCode()));
        assertTrue("Received refund amount is not equal to expected", refund.getAmount() == 1000L);
    }

    @Test
    public void addNewAdjustmentTest() throws Exception {
        List<InvoicingData.AdjustmentChangeStatusInfo> statusInfoList = new ArrayList<>();
        var captureStatus = new com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus();
        captureStatus.setCaptured(new InvoicePaymentAdjustmentCaptured());
        statusInfoList.add(new InvoicingData.AdjustmentChangeStatusInfo(PAYMENT_ID, ADJUSTMENT_ID, captureStatus));

        var pengingStatus = new com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus();
        pengingStatus.setPending(new InvoicePaymentAdjustmentPending());
        statusInfoList.add(new InvoicingData.AdjustmentChangeStatusInfo(PAYMENT_ID, "q-2", pengingStatus));

        List<InvoicingData.AdjustmentChangeStatusInfo> secondStatusInfoList = new ArrayList<>();
        var pengingStatusTwo = new com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus();
        pengingStatusTwo.setPending(new InvoicePaymentAdjustmentPending());
        secondStatusInfoList.add(new InvoicingData.AdjustmentChangeStatusInfo(PAYMENT_ID, "q-3", pengingStatusTwo));

        when(paymentMachineEventParser.parse(machineEventOne))
                .thenReturn(createTestAdjustmentEventPayload(statusInfoList));
        when(paymentMachineEventParser.parse(machineEventTwo))
                .thenReturn(createTestAdjustmentEventPayload(secondStatusInfoList));

        invoicingService.handleEvents(Arrays.asList(machineEventOne));
        invoicingService.handleEvents(Arrays.asList(machineEventTwo));

        List<Adjustment> adjustments = adjustmentDao.getAdjustmentsByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(AdjustmentStatus.cancelled, AdjustmentStatus.captured)
        );
        assertEquals("Received count of adjustments is not equal to expected", 1, adjustments.size());

        Adjustment adjustment = adjustments.get(0);
        assertTrue("Received adjustment is not equal to expected", INVOICE_ID.equals(adjustment.getInvoiceId()));
        assertTrue("Received adjustment is not equal to expected", PAYMENT_ID.equals(adjustment.getPaymentId()));
        assertTrue("Received adjustment is not equal to expected", ADJUSTMENT_ID.equals(adjustment.getAdjustmentId()));
        assertTrue("Received adjustment is not equal to expected",
                adjustment.getStatus() == AdjustmentStatus.captured);
        assertTrue("Received adjustment is not equal to expected", "RUR".equals(adjustment.getCurrencyCode()));
        assertTrue("Received adjustment is not equal to expected", adjustment.getAmount() == 2418L);
    }

}
