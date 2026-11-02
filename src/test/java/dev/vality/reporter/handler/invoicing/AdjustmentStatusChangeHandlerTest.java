package dev.vality.reporter.handler.invoicing;

import dev.vality.damsel.domain.FinalCashFlowPosting;
import dev.vality.damsel.domain.InvoicePaymentAdjustment;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.reporter.dao.AdjustmentDao;
import dev.vality.reporter.data.InvoicingTestData.*;
import dev.vality.reporter.domain.tables.pojos.Adjustment;
import dev.vality.reporter.service.FaultyEventsService;
import dev.vality.reporter.service.HellgateInvoicingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static dev.vality.reporter.data.AdjustmentTestData.*;
import static dev.vality.reporter.data.InvoicingTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentStatusChangeHandlerTest {

    private static final String INVOICE_ID = "invoice-id";
    private static final String PAYMENT_ID = "payment-id";

    @Mock
    private HellgateInvoicingService hgInvoicingService;
    @Mock
    private AdjustmentDao adjustmentDao;
    @Mock
    private FaultyEventsService faultyEventsService;
    @InjectMocks
    private AdjustmentStatusChangeHandler handler;

    private InvoicePayment invoicePayment;

    @BeforeEach
    void setUp() throws Exception {
        Invoice invoice = createHgInvoice(INVOICE_ID, PAYMENT_ID, "refund-id", "adjustment-id");
        invoicePayment = invoice.getPayments().get(0);
        when(hgInvoicingService.getInvoice(eq(INVOICE_ID), anyLong())).thenReturn(invoice);
    }

    @ParameterizedTest
    @CsvSource({
            "108791, 9791, 7243, 50111, 4510, 3336, 45601",
            "103941, 9355, 6920, 110100, 9909, 7330, 100191"
    })
    void savesOnlyStatusChangesAfterCashChangeOfFailedPayment(long oldAmount,
                                                             long oldFee,
                                                             long oldProviderFee,
                                                             long newAmount,
                                                             long newFee,
                                                             long newProviderFee,
                                                             long expectedMerchantAmount) throws Exception {
        invoicePayment.getPayment().setStatus(failedStatus());
        invoicePayment.getPayment().getCost().setAmount(newAmount);
        var cashFlowAdjustment = createAdjustment("1", cashFlowState(),
                inverse(cashFlow(oldAmount, oldFee, oldProviderFee)), List.of());
        handleAdjustment(cashFlowAdjustment, 15L);

        invoicePayment.getPayment().setStatus(capturedStatus());
        var captureAdjustment = createAdjustment("2", statusChangeState(capturedStatus()),
                List.of(), cashFlow(newAmount, newFee, newProviderFee));
        handleAdjustment(captureAdjustment, 18L);

        invoicePayment.getPayment().setStatus(failedStatus());
        var failAdjustment = createAdjustment("3", statusChangeState(failedStatus()),
                inverse(cashFlow(newAmount, newFee, newProviderFee)), List.of());
        handleAdjustment(failAdjustment, 21L);

        var captor = ArgumentCaptor.forClass(Adjustment.class);
        verify(adjustmentDao, times(2)).saveAdjustment(captor.capture());
        List<Adjustment> saved = captor.getAllValues();
        assertEquals("2", saved.get(0).getAdjustmentId());
        assertEquals(expectedMerchantAmount, saved.get(0).getAmount().longValue());
        assertEquals("3", saved.get(1).getAdjustmentId());
        assertEquals(-expectedMerchantAmount, saved.get(1).getAmount().longValue());
    }

    @Test
    void skipsCashFlowAdjustmentWithZeroAmountForFailedPayment() throws Exception {
        invoicePayment.getPayment().setStatus(failedStatus());
        handleAdjustment(createAdjustment("1", cashFlowState(), List.of(), List.of()), 15L);

        verifyNoInteractions(adjustmentDao);
    }

    @Test
    void savesCashFlowAdjustmentForCapturedPayment() throws Exception {
        invoicePayment.getPayment().setStatus(capturedStatus());
        var adjustment = createAdjustment("1", cashFlowState(),
                inverse(cashFlow(100000L, 10000L, 7000L)), cashFlow(120000L, 12000L, 8400L));
        handleAdjustment(adjustment, 15L);

        var captor = ArgumentCaptor.forClass(Adjustment.class);
        verify(adjustmentDao).saveAdjustment(captor.capture());
        assertEquals(18000L, captor.getValue().getAmount().longValue());
    }

    @Test
    void preservesAdjustmentWithoutStateForFailedPayment() throws Exception {
        invoicePayment.getPayment().setStatus(failedStatus());
        var adjustment = createAdjustment("1", null,
                inverse(cashFlow(100000L, 10000L, 7000L)), List.of());
        handleAdjustment(adjustment, 15L);

        var captor = ArgumentCaptor.forClass(Adjustment.class);
        verify(adjustmentDao).saveAdjustment(captor.capture());
        assertEquals(-90000L, captor.getValue().getAmount().longValue());
    }

    private void handleAdjustment(InvoicePaymentAdjustment adjustment, long eventId) throws Exception {
        invoicePayment.setAdjustments(List.of(adjustment));
        var event = createMachineEvent(INVOICE_ID).setEventId(eventId);
        var payload = createTestAdjustmentEventPayload(List.of(
                new AdjustmentChangeStatusInfo(PAYMENT_ID, adjustment.getId(), adjustment.getStatus())));
        handler.handle(createKafkaEvent(event), payload.getInvoiceChanges().get(0), 0);
    }

    private static List<FinalCashFlowPosting> inverse(List<FinalCashFlowPosting> cashFlow) {
        return cashFlow.stream()
                .map(posting -> new FinalCashFlowPosting()
                        .setSource(posting.getDestination())
                        .setDestination(posting.getSource())
                        .setVolume(posting.getVolume()))
                .toList();
    }

}
