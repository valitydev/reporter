package com.rbkmoney.reporter.data;

import com.rbkmoney.damsel.base.Content;
import com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentPending;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.reporter.model.KafkaEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InvoicingData {

    private static final String PARTY_ID = "patry-id-1";
    private static final String SHOP_ID = "shop-id-1";
    private static final String EXTERNAL_ID = "external-1";
    private static final String INVOICE_ID = "inv-1";
    private static final String INVOICE_ID_2 = "inv-2";
    private static final String PAYMENT_ID = "pay-1";
    private static final String REFUND_ID = "ref-1";
    private static final String ADJUSTMENT_ID = "adj-1";

    public static EventPayload createTestInvoiceEventPayload(List<InvoiceChangeStatusInfo> statusInfoList) {
        EventPayload eventPayload = new EventPayload();
        eventPayload.setInvoiceChanges(createInvoiceChangeList(statusInfoList));
        return eventPayload;
    }

    public static EventPayload createTestPaymentEventPayload(List<PaymentChangeStatusInfo> statusInfoList) {
        EventPayload eventPayload = new EventPayload();
        eventPayload.setInvoiceChanges(createPaymentChangeList(statusInfoList));
        return eventPayload;
    }


    public static EventPayload createTestRefundEventPayload(List<RefundChangeStatusInfo> statusInfoList) {
        EventPayload eventPayload = new EventPayload();
        eventPayload.setInvoiceChanges(createRefundsChangeList(statusInfoList));
        return eventPayload;
    }


    public static EventPayload createTestAdjustmentEventPayload(List<AdjustmentChangeStatusInfo> statusInfoList) {
        EventPayload eventPayload = new EventPayload();
        eventPayload.setInvoiceChanges(createAdjustmentsChangeList(statusInfoList));
        return eventPayload;
    }

    private static List<InvoiceChange> createInvoiceChangeList(List<InvoiceChangeStatusInfo> statusInfoList) {
        List<InvoiceChange> invoiceChanges = new ArrayList<>();

        for (InvoiceChangeStatusInfo statusInfo : statusInfoList) {
            for (int i = 0; i < statusInfo.getCount(); i++) {
                invoiceChanges.add(createInvoiceStatusChange(statusInfo.getStatus()));
            }
        }
        return invoiceChanges;
    }

    private static List<InvoiceChange> createPaymentChangeList(List<PaymentChangeStatusInfo> statusInfoList) {
        List<InvoiceChange> invoiceChanges = new ArrayList<>();

        for (PaymentChangeStatusInfo statusInfo : statusInfoList) {
            invoiceChanges.add(createPaymentStatusChange(statusInfo.getPaymentId(), statusInfo.getStatus()));
        }
        return invoiceChanges;
    }

    private static List<InvoiceChange> createRefundsChangeList(List<RefundChangeStatusInfo> statusInfoList) {
        List<InvoiceChange> invoiceChanges = new ArrayList<>();

        for (RefundChangeStatusInfo statusInfo : statusInfoList) {
            invoiceChanges.add(createRefundStatusChange(
                    statusInfo.getPaymentId(),
                    statusInfo.getRefundId(),
                    statusInfo.getStatus())
            );
        }
        return invoiceChanges;
    }

    private static List<InvoiceChange> createAdjustmentsChangeList(List<AdjustmentChangeStatusInfo> statusInfoList) {
        List<InvoiceChange> invoiceChanges = new ArrayList<>();

        for (AdjustmentChangeStatusInfo statusInfo : statusInfoList) {
            invoiceChanges.add(createAdjustmentStatusChange(
                    statusInfo.getPaymentId(),
                    statusInfo.getAdjustmentId(),
                    statusInfo.getStatus())
            );
        }
        return invoiceChanges;
    }

    public static InvoiceChange createInvoiceStatusChange(com.rbkmoney.reporter.domain.enums.InvoiceStatus status) {
        InvoiceChange invoiceChange = new InvoiceChange();
        InvoiceStatusChanged statusChanged = new InvoiceStatusChanged();
        InvoiceStatus invoiceStatus = new InvoiceStatus();
        switch (status) {
            case paid:
                invoiceStatus.setPaid(new InvoicePaid());
                break;
            case unpaid:
                invoiceStatus.setUnpaid(new InvoiceUnpaid());
                break;
            case cancelled:
                invoiceStatus.setCancelled(new InvoiceCancelled());
                break;
            case fulfilled:
            default:
                invoiceStatus.setFulfilled(new InvoiceFulfilled());
                break;
        }
        statusChanged.setStatus(invoiceStatus);
        invoiceChange.setInvoiceStatusChanged(statusChanged);
        return invoiceChange;
    }

    private static InvoiceChange createPaymentStatusChange(String paymentId,
                                                           com.rbkmoney.damsel.domain.InvoicePaymentStatus status) {
        InvoicePaymentChange invoicePaymentChange = new InvoicePaymentChange();
        invoicePaymentChange.setId(paymentId);

        InvoicePaymentChangePayload payload = new InvoicePaymentChangePayload();
        InvoicePaymentStatusChanged statusChanged = new InvoicePaymentStatusChanged();
        statusChanged.setStatus(status);
        payload.setInvoicePaymentStatusChanged(statusChanged);
        invoicePaymentChange.setPayload(payload);
        InvoiceChange invoiceChange = new InvoiceChange();
        invoiceChange.setInvoicePaymentChange(invoicePaymentChange);
        return invoiceChange;
    }

    private static InvoiceChange createRefundStatusChange(
            String paymentId,
            String refundId,
            com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus status
    ) {
        InvoicePaymentChange invoicePaymentChange = new InvoicePaymentChange();
        invoicePaymentChange.setId(paymentId);
        InvoicePaymentRefundChange invoicePaymentRefundChange = new InvoicePaymentRefundChange();

        invoicePaymentRefundChange.setId(refundId);
        InvoicePaymentRefundChangePayload refundChangePayload = new InvoicePaymentRefundChangePayload();
        InvoicePaymentRefundStatusChanged refundStatusChanged = new InvoicePaymentRefundStatusChanged();
        refundStatusChanged.setStatus(status);
        refundChangePayload.setInvoicePaymentRefundStatusChanged(refundStatusChanged);
        invoicePaymentRefundChange.setPayload(refundChangePayload);
        InvoicePaymentChangePayload payload = new InvoicePaymentChangePayload();
        payload.setInvoicePaymentRefundChange(invoicePaymentRefundChange);
        invoicePaymentChange.setPayload(payload);
        InvoiceChange invoiceChange = new InvoiceChange();
        invoiceChange.setInvoicePaymentChange(invoicePaymentChange);
        return invoiceChange;
    }

    private static InvoiceChange createAdjustmentStatusChange(
            String paymentId,
            String adjustmentId,
            com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus status
    ) {
        InvoicePaymentChange invoicePaymentChange = new InvoicePaymentChange();
        invoicePaymentChange.setId(paymentId);
        InvoicePaymentAdjustmentChange adjustmentChange = new InvoicePaymentAdjustmentChange();

        adjustmentChange.setId(adjustmentId);
        InvoicePaymentAdjustmentChangePayload adjustmentChangePayload = new InvoicePaymentAdjustmentChangePayload();
        InvoicePaymentAdjustmentStatusChanged statusChanged = new InvoicePaymentAdjustmentStatusChanged();
        statusChanged.setStatus(status);
        adjustmentChangePayload.setInvoicePaymentAdjustmentStatusChanged(statusChanged);
        adjustmentChange.setPayload(adjustmentChangePayload);

        InvoicePaymentChangePayload payload = new InvoicePaymentChangePayload();
        payload.setInvoicePaymentAdjustmentChange(adjustmentChange);
        invoicePaymentChange.setPayload(payload);
        InvoiceChange invoiceChange = new InvoiceChange();
        invoiceChange.setInvoicePaymentChange(invoicePaymentChange);
        return invoiceChange;
    }

    public static KafkaEvent createKafkaEvent(MachineEvent machineEvent) {
        return new KafkaEvent("test", 1, 1, machineEvent);
    }

    public static KafkaEvent createKafkaEvent(String invoiceId) {
        return new KafkaEvent("test", 1, 1, createMachineEvent(invoiceId));
    }

    public static MachineEvent createMachineEvent(String invoiceId) {
        MachineEvent message = new MachineEvent();
        var data = new com.rbkmoney.machinegun.msgpack.Value();
        data.setBin(new byte[0]);
        message.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        message.setEventId(1L);
        message.setSourceNs("sad");
        message.setSourceId(invoiceId);
        message.setData(data);
        return message;
    }

    public static com.rbkmoney.damsel.payment_processing.Invoice createHgInvoice(String invoiceId,
                                                                                 String paymentId,
                                                                                 String refundId,
                                                                                 String adjustmentId) {
        var invoice = new com.rbkmoney.damsel.payment_processing.Invoice();
        invoice.setInvoice(createDomainInvoice(invoiceId));
        List<com.rbkmoney.damsel.payment_processing.InvoicePayment> invoicePaymentList = new ArrayList<>();
        invoicePaymentList.add(createInvoicePayment(paymentId, refundId, adjustmentId));
        invoice.setPayments(invoicePaymentList);
        return invoice;
    }

    private static com.rbkmoney.damsel.domain.Invoice createDomainInvoice(String invoiceId) {
        var hgInvoice = new com.rbkmoney.damsel.domain.Invoice();
        hgInvoice.setId(invoiceId);
        hgInvoice.setContext(new Content());
        Cash cash = new Cash();
        cash.setAmount(1000L);
        cash.setCurrency(new CurrencyRef().setSymbolicCode("RUR"));
        hgInvoice.setCost(cash);
        hgInvoice.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        hgInvoice.setDetails(new InvoiceDetails()
                .setCart(new InvoiceCart().setLines(new ArrayList<>()))
                .setDescription("Desc-1")
                .setProduct("Product-1")
        );
        hgInvoice.setOwnerId(PARTY_ID);
        hgInvoice.setShopId(SHOP_ID);
        hgInvoice.setDue(TypeUtil.temporalToString(LocalDateTime.now()));
        hgInvoice.setExternalId(EXTERNAL_ID);
        hgInvoice.setPartyRevision(1);
        hgInvoice.setStatus(InvoiceStatus.paid(new InvoicePaid()));
        hgInvoice.setTemplateId("1");
        return hgInvoice;
    }

    private static com.rbkmoney.damsel.payment_processing.InvoicePayment createInvoicePayment(
            String paymentId,
            String refundId,
            String adjustmentId
    ) {
        var invoicePayment = new com.rbkmoney.damsel.payment_processing.InvoicePayment();
        invoicePayment.setPayment(createDamselInvoicePayment(paymentId));
        invoicePayment.setAdjustments(createInvoicePaymentAdjustmentList(adjustmentId));
        invoicePayment.setRefunds(createInvoicePaymentRefundList(refundId));
        invoicePayment.setCashFlow(createCashFlowList());
        invoicePayment.setRoute(createRoute());
        invoicePayment.setSessions(createSessions());
        invoicePayment.setChargebacks(new ArrayList<>());
        invoicePayment.setLegacyRefunds(new ArrayList<>());
        return invoicePayment;
    }

    private static com.rbkmoney.damsel.domain.InvoicePayment createDamselInvoicePayment(String paymentId) {
        var hgPayment = new com.rbkmoney.damsel.domain.InvoicePayment();
        hgPayment.setId(paymentId);
        hgPayment.setOwnerId(PARTY_ID);
        hgPayment.setShopId(SHOP_ID);
        hgPayment.setExternalId(EXTERNAL_ID);
        hgPayment.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        hgPayment.setStatus(InvoicePaymentStatus.captured(new InvoicePaymentCaptured()));
        Cash cash = new Cash();
        cash.setAmount(1000L);
        cash.setCurrency(new CurrencyRef().setSymbolicCode("RUR"));
        hgPayment.setCost(cash);
        hgPayment.setDomainRevision(1);
        hgPayment.setFlow(InvoicePaymentFlow.instant(new InvoicePaymentFlowInstant()));
        Payer payer = new Payer();
        PaymentResourcePayer paymentResourcePayer = new PaymentResourcePayer();
        paymentResourcePayer.setResource(new DisposablePaymentResource()
                .setClientInfo(new ClientInfo()
                        .setIpAddress("127.0.0.1")
                        .setFingerprint("fp"))
                .setPaymentSessionId("123")
                .setPaymentTool(PaymentTool.digital_wallet(new DigitalWallet()
                        .setId("123")
                        .setProviderDeprecated(LegacyDigitalWalletProvider.rbkmoney)))
        );
        payer.setPaymentResource(paymentResourcePayer);
        hgPayment.setPayer(payer);
        hgPayment.setContext(new Content());
        return hgPayment;
    }

    private static List<com.rbkmoney.damsel.domain.InvoicePaymentAdjustment> createInvoicePaymentAdjustmentList(
            String defaultAdjustmentId
    ) {
        List<com.rbkmoney.damsel.domain.InvoicePaymentAdjustment> adjList = new ArrayList<>();
        adjList.add(createInvoicePaymentAdjustment(
                "1",
                InvoicePaymentAdjustmentStatus.pending(new InvoicePaymentAdjustmentPending()))
        );
        adjList.add(createInvoicePaymentAdjustment(
                defaultAdjustmentId,
                InvoicePaymentAdjustmentStatus.captured(new InvoicePaymentAdjustmentCaptured()))
        );
        return adjList;
    }

    private static com.rbkmoney.damsel.domain.InvoicePaymentAdjustment createInvoicePaymentAdjustment(
            String id,
            InvoicePaymentAdjustmentStatus status
    ) {
        var adj = new com.rbkmoney.damsel.domain.InvoicePaymentAdjustment();
        adj.setId(id);
        adj.setStatus(status);
        adj.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        adj.setDomainRevision(1);
        adj.setReason("some reason");
        adj.setNewCashFlow(createNewCashFlow());
        adj.setOldCashFlowInverse(createOldCashFlow());
        return adj;
    }

    public static List<FinalCashFlowPosting> createPaymentCashFlow() {
        return List.of(
                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945).setCurrency(new CurrencyRef().setSymbolicCode("RUR"))),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500).setCurrency(new CurrencyRef().setSymbolicCode("RUR"))),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923).setCurrency(new CurrencyRef().setSymbolicCode("RUR")))
        );
    }

    public static List<FinalCashFlowPosting> createNewCashFlow() {
        return List.of(
                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945).setCurrency(new CurrencyRef().setSymbolicCode("RUR"))),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500).setCurrency(new CurrencyRef().setSymbolicCode("RUR"))),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(14505).setCurrency(new CurrencyRef().setSymbolicCode("RUR")))
        );
    }

    public static List<FinalCashFlowPosting> createOldCashFlow() {
        return List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945).setCurrency(new CurrencyRef().setSymbolicCode("RUR"))),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500).setCurrency(new CurrencyRef().setSymbolicCode("RUR"))),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount()
                                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923).setCurrency(new CurrencyRef().setSymbolicCode("RUR")))
        );
    }

    private static List<com.rbkmoney.damsel.payment_processing.InvoicePaymentRefund> createInvoicePaymentRefundList(
            String defaultRefundId
    ) {
        List<com.rbkmoney.damsel.payment_processing.InvoicePaymentRefund> refundList = new ArrayList<>();
        refundList.add(createInvoicePaymentRefund(
                defaultRefundId,
                InvoicePaymentRefundStatus.succeeded(new InvoicePaymentRefundSucceeded()))
        );
        return refundList;
    }

    private static com.rbkmoney.damsel.payment_processing.InvoicePaymentRefund createInvoicePaymentRefund(
            String id,
            InvoicePaymentRefundStatus status
    ) {
        var hgRefund = new com.rbkmoney.damsel.domain.InvoicePaymentRefund();
        hgRefund.setId(id);
        hgRefund.setStatus(status);
        hgRefund.setExternalId(EXTERNAL_ID);
        hgRefund.setPartyRevision(1L);

        Cash cash = new Cash();
        cash.setAmount(1000L);
        cash.setCurrency(new CurrencyRef().setSymbolicCode("RUR"));
        hgRefund.setCash(cash);
        hgRefund.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        hgRefund.setDomainRevision(1);
        var refund = new com.rbkmoney.damsel.payment_processing.InvoicePaymentRefund();
        refund.setRefund(hgRefund);
        refund.setSessions(createInvoiceRefundSessionList());
        refund.setCashFlow(createCashFlowList());
        return refund;
    }

    private static List<InvoiceRefundSession> createInvoiceRefundSessionList() {
        List<InvoiceRefundSession> invoiceRefundSessionList = new ArrayList<>();
        invoiceRefundSessionList.add(createInvoiceRefundSession());
        return invoiceRefundSessionList;
    }

    private static InvoiceRefundSession createInvoiceRefundSession() {
        InvoiceRefundSession refundSession = new InvoiceRefundSession();
        refundSession.setTransactionInfo(createTransactionInfo(true, "refund"));
        return refundSession;
    }

    private static List<com.rbkmoney.damsel.domain.FinalCashFlowPosting> createCashFlowList() {
        return createNewCashFlow();
    }

    private static com.rbkmoney.damsel.domain.PaymentRoute createRoute() {
        var paymentRoute = new com.rbkmoney.damsel.domain.PaymentRoute();
        paymentRoute.setProvider(new ProviderRef().setId(123));
        paymentRoute.setTerminal(new TerminalRef().setId(222));
        return paymentRoute;
    }

    private static List<InvoicePaymentSession> createSessions() {
        List<InvoicePaymentSession> invoicePaymentSessionList = new ArrayList<>();
        invoicePaymentSessionList.add(createSession(
                com.rbkmoney.damsel.domain.TargetInvoicePaymentStatus.processed(new InvoicePaymentProcessed()),
                false)
        );
        invoicePaymentSessionList.add(createSession(
                com.rbkmoney.damsel.domain.TargetInvoicePaymentStatus.processed(new InvoicePaymentProcessed()),
                true)
        );
        invoicePaymentSessionList.add(createSession(
                com.rbkmoney.damsel.domain.TargetInvoicePaymentStatus.captured(new InvoicePaymentCaptured()),
                true)
        );
        return invoicePaymentSessionList;
    }

    private static InvoicePaymentSession createSession(com.rbkmoney.damsel.domain.TargetInvoicePaymentStatus status,
                                                       boolean isSetAdditionalInfo) {
        InvoicePaymentSession session = new InvoicePaymentSession();
        session.setTargetStatus(status);
        session.setTransactionInfo(createTransactionInfo(isSetAdditionalInfo, status.getSetField().getFieldName()));
        return session;
    }

    private static TransactionInfo createTransactionInfo(boolean isSetAdditionalInfo, String approvalCode) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setId("trxInfoId-1");
        transactionInfo.setTimestamp(TypeUtil.temporalToString(LocalDateTime.now()));
        transactionInfo.setExtra(new HashMap<>());
        if (isSetAdditionalInfo) {
            transactionInfo.setAdditionalInfo(createAdditionalTransactionInfo(approvalCode));
        }
        return transactionInfo;
    }

    private static AdditionalTransactionInfo createAdditionalTransactionInfo(
            String approvalCode
    ) {
        AdditionalTransactionInfo transactionInfo = new AdditionalTransactionInfo();
        transactionInfo.setRrn("rrn");
        transactionInfo.setApprovalCode(approvalCode);
        transactionInfo.setAcsUrl("acs");
        transactionInfo.setPareq("pareq");
        transactionInfo.setMd("md");
        transactionInfo.setTermUrl("term");
        transactionInfo.setPares("pares");
        transactionInfo.setEci("eci");
        transactionInfo.setCavv("cavv");
        transactionInfo.setXid("xid");
        transactionInfo.setCavvAlgorithm("algorithm");
        transactionInfo.setThreeDsVerification(ThreeDsVerification.authentication_successful);
        return transactionInfo;
    }

    @Data
    @AllArgsConstructor
    public static class InvoiceChangeStatusInfo {

        private int count;
        private com.rbkmoney.reporter.domain.enums.InvoiceStatus status;

    }

    @Data
    @AllArgsConstructor
    public static class PaymentChangeStatusInfo {

        private String paymentId;
        private com.rbkmoney.damsel.domain.InvoicePaymentStatus status;

    }

    @Data
    @AllArgsConstructor
    public static class RefundChangeStatusInfo {

        private String paymentId;
        private String refundId;
        private com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus status;

    }

    @Data
    @AllArgsConstructor
    public static class AdjustmentChangeStatusInfo {

        private String paymentId;
        private String adjustmentId;
        private com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus status;

    }

}
