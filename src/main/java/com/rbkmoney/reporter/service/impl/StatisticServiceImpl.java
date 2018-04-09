package com.rbkmoney.reporter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.reporter.dsl.DslUtil;
import com.rbkmoney.reporter.exception.InvoiceNotFoundException;
import com.rbkmoney.reporter.exception.PaymentNotFoundException;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.StatisticService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StatisticServiceImpl implements StatisticService {

    private final MerchantStatisticsSrv.Iface merchantStatisticsClient;

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    private final ObjectMapper objectMapper;

    @Autowired
    public StatisticServiceImpl(MerchantStatisticsSrv.Iface merchantStatisticsClient, ObjectMapper objectMapper) {
        this.merchantStatisticsClient = merchantStatisticsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ShopAccountingModel getShopAccounting(String partyId, String contractId, String currencyCode, Instant toTime) {
        return getShopAccounting(partyId, contractId, currencyCode, Optional.empty(), toTime);
    }

    @Override
    public ShopAccountingModel getShopAccounting(String partyId, String contractId, String currencyCode, Instant fromTime, Instant toTime) {
        return getShopAccounting(partyId, contractId, currencyCode, Optional.of(fromTime), toTime);
    }

    @Override
    public List<StatInvoice> getInvoices(String partyId, String contractId, Instant fromTime, Instant toTime) {
        try {
            long from = 0;
            int size = 1000;
            List<StatInvoice> invoices = new ArrayList<>();
            List<StatInvoice> nextInvoices;
            do {
                StatResponse statResponse = merchantStatisticsClient.getInvoices(DslUtil.createInvoicesRequest(partyId, contractId, fromTime, toTime, from, size, objectMapper));
                nextInvoices = statResponse.getData().getInvoices();
                invoices.addAll(nextInvoices);
                from += size;
            } while (nextInvoices.size() == size);
            return invoices;
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
        public StatInvoice getInvoice(String invoiceId) {
        try {
            return merchantStatisticsClient.getPayments(DslUtil.createInvoiceRequest(invoiceId, objectMapper))
                    .getData()
                    .getInvoices()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new InvoiceNotFoundException(String.format("Invoice with id={}  not found", invoiceId)));
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ShopAccountingModel getShopAccounting(String partyId, String contractId, String currencyCode, Optional<Instant> fromTime, Instant toTime) {
        try {
            ShopAccountingModel shopAccounting = merchantStatisticsClient.getStatistics(
                    DslUtil.createShopAccountingStatRequest(partyId, contractId, currencyCode, fromTime, toTime, objectMapper)
            ).getData()
                    .getRecords()
                    .stream()
                    .map(record -> objectMapper.convertValue(record, ShopAccountingModel.class))
                    .findFirst()
                    .orElse(new ShopAccountingModel(partyId, contractId, currencyCode));
            validate(shopAccounting);
            return shopAccounting;
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<StatPayment> getPayments(String partyId, String contractId, Instant fromTime, Instant toTime, InvoicePaymentStatus status) {
        try {
            long from = 0;
            int size = 1000;
            List<StatPayment> payments = new ArrayList<>();
            List<StatPayment> nextPayments;
            do {
                StatResponse statResponse = merchantStatisticsClient.getPayments(DslUtil.createPaymentsRequest(partyId, contractId, fromTime, toTime, status, from, size, objectMapper));
                nextPayments = statResponse.getData().getPayments();
                payments.addAll(nextPayments);
                from += size;
            } while (nextPayments.size() == size);
            return payments;
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public StatPayment getPayment(String invoiceId, String paymentId, InvoicePaymentStatus status) {
        try {
            return merchantStatisticsClient.getPayments(DslUtil.createPaymentRequest(invoiceId, paymentId, status, objectMapper))
                    .getData()
                    .getPayments()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new PaymentNotFoundException(String.format("Payment with id={}.{} and status={} not found", invoiceId, paymentId, status.getSetField().getFieldName())));
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<StatRefund> getRefunds(String partyId, String contractId, Instant fromTime, Instant toTime, InvoicePaymentRefundStatus status) {
        try {
            long from = 0;
            int size = 1000;
            List<StatRefund> refunds = new ArrayList<>();
            List<StatRefund> nextRefunds;
            do {
                StatResponse statResponse = merchantStatisticsClient.getPayments(DslUtil.createRefundsRequest(partyId, contractId, fromTime, toTime, status, from, size, objectMapper));
                nextRefunds = statResponse.getData().getRefunds();
                refunds.addAll(nextRefunds);
                from += size;
            } while (nextRefunds.size() == size);
            return refunds;
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    private <T> void validate(T model) {
        Set<ConstraintViolation<T>> constraintViolations = factory.getValidator().validate(model);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }
}
