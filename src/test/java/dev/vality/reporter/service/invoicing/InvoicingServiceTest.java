package dev.vality.reporter.service.invoicing;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.reporter.config.testconfiguration.MockedUnimportantServicesConfig;
import dev.vality.reporter.dao.AdjustmentDao;
import dev.vality.reporter.dao.InvoiceDao;
import dev.vality.reporter.dao.PaymentDao;
import dev.vality.reporter.dao.RefundDao;
import dev.vality.reporter.data.InvoicingData;
import dev.vality.reporter.domain.enums.AdjustmentStatus;
import dev.vality.reporter.domain.enums.InvoiceStatus;
import dev.vality.reporter.domain.enums.RefundStatus;
import dev.vality.reporter.domain.tables.pojos.Adjustment;
import dev.vality.reporter.domain.tables.pojos.Invoice;
import dev.vality.reporter.domain.tables.pojos.Payment;
import dev.vality.reporter.domain.tables.pojos.Refund;
import dev.vality.reporter.handler.invoicing.InvoiceStatusChangeHandler;
import dev.vality.reporter.service.HellgateInvoicingService;
import dev.vality.reporter.service.impl.InvoicingService;
import dev.vality.sink.common.parser.Parser;
import dev.vality.testcontainers.annotations.DefaultSpringBootTest;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.vality.reporter.data.InvoicingData.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@PostgresqlTestcontainer
@DefaultSpringBootTest
@Import(MockedUnimportantServicesConfig.class)
public class InvoicingServiceTest {

    private static final String INVOICE_ID = "inv-1";
    private static final String INVOICE_ID_2 = "inv-2";
    private static final String PAYMENT_ID = "pay-1";
    private static final String REFUND_ID = "ref-1";
    private static final String ADJUSTMENT_ID = "adj-1";
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
    private HellgateInvoicingService hgInvoicingService;
    private final MachineEvent machineEventOne = createMachineEvent(INVOICE_ID);
    private final MachineEvent machineEventTwo = createMachineEvent(INVOICE_ID_2);

    @BeforeEach
    public void init() throws Exception {
        when(hgInvoicingService.getInvoice(INVOICE_ID, 1L))
                .thenReturn(createHgInvoice(INVOICE_ID, PAYMENT_ID, REFUND_ID, ADJUSTMENT_ID));
        when(hgInvoicingService.getInvoice(INVOICE_ID_2, 1L))
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

        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventOne)));
        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventTwo)));
        List<Invoice> invoices = invoiceDao.getInvoicesByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(InvoiceStatus.paid, InvoiceStatus.unpaid, InvoiceStatus.cancelled,
                        InvoiceStatus.fulfilled)
        );
        assertEquals("Count of invoices is not equal to expected", 1, invoices.size());
        Invoice invoice = invoices.get(0);
        assertEquals("Invoice id is not equal to expected", INVOICE_ID, invoice.getInvoiceId());
        assertEquals("Invoice status is not equal to expected", InvoiceStatus.paid, invoice.getStatus());
        assertEquals("Invoice currency is not equal to expected", "RUR", invoice.getCurrencyCode());
        assertEquals("Invoice amount is not equal to expected", Long.valueOf(1000L), invoice.getAmount());
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

        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventOne)));
        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventTwo)));
        List<Payment> payments = paymentDao.getPaymentsByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(dev.vality.reporter.domain.enums.InvoicePaymentStatus.captured,
                        dev.vality.reporter.domain.enums.InvoicePaymentStatus.cancelled,
                        dev.vality.reporter.domain.enums.InvoicePaymentStatus.failed)
        );
        assertEquals("Received count of invoices is not equal to expected", 1, payments.size());
        Payment payment = payments.get(0);
        assertEquals("Payment invoice id is not equal to expected", INVOICE_ID, payment.getInvoiceId());
        assertEquals("Payment id is not equal to expected", PAYMENT_ID, payment.getPaymentId());
        assertEquals("Payment status is not equal to expected",
                dev.vality.reporter.domain.enums.InvoicePaymentStatus.captured, payment.getStatus());
        assertEquals("Payment currency is not equal to expected", "RUR", payment.getCurrencyCode());
        assertEquals("Payment amount is not equal to expected", Long.valueOf(1000L), payment.getAmount());
    }

    @Test
    public void addNewRefundTest() throws Exception {
        List<InvoicingData.RefundChangeStatusInfo> firstStatusInfoList = new ArrayList<>();
        var captureStatus = new dev.vality.damsel.domain.InvoicePaymentRefundStatus();
        captureStatus.setSucceeded(new InvoicePaymentRefundSucceeded());
        firstStatusInfoList.add(new InvoicingData.RefundChangeStatusInfo(PAYMENT_ID, REFUND_ID, captureStatus));
        var pendingStatus = new dev.vality.damsel.domain.InvoicePaymentRefundStatus();
        pendingStatus.setPending(new InvoicePaymentRefundPending());
        firstStatusInfoList.add(new InvoicingData.RefundChangeStatusInfo(PAYMENT_ID, "2", pendingStatus));

        List<InvoicingData.RefundChangeStatusInfo> secondStatusInfoList = new ArrayList<>();
        var pendingStatusTwo = new dev.vality.damsel.domain.InvoicePaymentRefundStatus();
        pendingStatusTwo.setPending(new InvoicePaymentRefundPending());
        secondStatusInfoList.add(new InvoicingData.RefundChangeStatusInfo(PAYMENT_ID, REFUND_ID, pendingStatusTwo));

        when(paymentMachineEventParser.parse(machineEventOne))
                .thenReturn(createTestRefundEventPayload(firstStatusInfoList));
        when(paymentMachineEventParser.parse(machineEventTwo))
                .thenReturn(createTestRefundEventPayload(secondStatusInfoList));

        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventOne)));
        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventTwo)));

        List<Refund> refunds = refundDao.getRefundsByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(RefundStatus.failed, RefundStatus.succeeded)
        );
        assertEquals("Count of refunds is not equal to expected", 1, refunds.size());

        Refund refund = refunds.get(0);
        assertEquals("Refund invoice id is not equal to expected", INVOICE_ID, refund.getInvoiceId());
        assertEquals("Refund payment id is not equal to expected", PAYMENT_ID, refund.getPaymentId());
        assertEquals("Refund id is not equal to expected", REFUND_ID, refund.getRefundId());
        assertEquals("Refund status is not equal to expected", RefundStatus.succeeded, refund.getStatus());
        assertEquals("Refund currency is not equal to expected", "RUR", refund.getCurrencyCode());
        assertEquals("Refund amount is not equal to expected", Long.valueOf(1000L), refund.getAmount());
    }

    @Test
    public void addNewAdjustmentTest() throws Exception {
        List<InvoicingData.AdjustmentChangeStatusInfo> statusInfoList = new ArrayList<>();
        var captureStatus = new dev.vality.damsel.domain.InvoicePaymentAdjustmentStatus();
        captureStatus.setCaptured(new InvoicePaymentAdjustmentCaptured());
        statusInfoList.add(
                new InvoicingData.AdjustmentChangeStatusInfo(PAYMENT_ID, ADJUSTMENT_ID, captureStatus));

        var pengingStatus = new dev.vality.damsel.domain.InvoicePaymentAdjustmentStatus();
        pengingStatus.setPending(new InvoicePaymentAdjustmentPending());
        statusInfoList.add(
                new InvoicingData.AdjustmentChangeStatusInfo(PAYMENT_ID, "q-2", pengingStatus));

        List<InvoicingData.AdjustmentChangeStatusInfo> secondStatusInfoList = new ArrayList<>();
        var pengingStatusTwo = new dev.vality.damsel.domain.InvoicePaymentAdjustmentStatus();
        pengingStatusTwo.setPending(new InvoicePaymentAdjustmentPending());
        secondStatusInfoList.add(
                new InvoicingData.AdjustmentChangeStatusInfo(PAYMENT_ID, "q-3", pengingStatusTwo));

        when(paymentMachineEventParser.parse(machineEventOne))
                .thenReturn(createTestAdjustmentEventPayload(statusInfoList));
        when(paymentMachineEventParser.parse(machineEventTwo))
                .thenReturn(createTestAdjustmentEventPayload(secondStatusInfoList));

        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventOne)));
        invoicingService.handleEvents(Arrays.asList(createKafkaEvent(machineEventTwo)));

        List<Adjustment> adjustments = adjustmentDao.getAdjustmentsByState(
                LocalDateTime.now().minus(10L, ChronoUnit.MINUTES),
                LocalDateTime.now(),
                Arrays.asList(AdjustmentStatus.cancelled, AdjustmentStatus.captured)
        );
        assertEquals("Count of adjustments is not equal to expected", 1, adjustments.size());

        Adjustment adjustment = adjustments.get(0);
        assertEquals("Adjustment invoice id is not equal to expected",
                INVOICE_ID, adjustment.getInvoiceId());
        assertEquals("Adjustment payment id is not equal to expected",
                PAYMENT_ID, adjustment.getPaymentId());
        assertEquals("Adjustment adjustment id is not equal to expected",
                ADJUSTMENT_ID, adjustment.getAdjustmentId());
        assertEquals("Adjustment status is not equal to expected",
                AdjustmentStatus.captured, adjustment.getStatus());
        assertEquals("Adjustment currency is not equal to expected",
                "RUR", adjustment.getCurrencyCode());
        assertEquals("Adjustment amount is not equal to expected",
                Long.valueOf(2418L), adjustment.getAmount());
    }
}
