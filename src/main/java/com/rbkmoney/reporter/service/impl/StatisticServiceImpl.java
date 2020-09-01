package com.rbkmoney.reporter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.reporter.dsl.DslUtil;
import com.rbkmoney.reporter.exception.InvoiceNotFoundException;
import com.rbkmoney.reporter.exception.PaymentNotFoundException;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final MerchantStatisticsSrv.Iface merchantStatisticsClient;

    private final ObjectMapper objectMapper;

    private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    @Override
    public ShopAccountingModel getShopAccounting(String partyId, String shopId, String currencyCode, Instant toTime) {
        return getShopAccounting(partyId, shopId, currencyCode, Optional.empty(), toTime);
    }

    @Override
    public ShopAccountingModel getShopAccounting(String partyId, String shopId, String currencyCode, Instant fromTime, Instant toTime) {
        return getShopAccounting(partyId, shopId, currencyCode, Optional.of(fromTime), toTime);
    }

    @Override
    public Map<String, String> getPurposes(String partyId, String shopId, Instant fromTime, Instant toTime) {
        try {
            Optional<String> continuationToken = Optional.empty();
            int size = 1000;
            Map<String, String> purposes = new HashMap<>();
            List<StatInvoice> nextInvoices;
            do {
                StatResponse statResponse = merchantStatisticsClient.getInvoices(DslUtil.createInvoicesRequest(partyId, shopId, fromTime, toTime, continuationToken, size, objectMapper));
                nextInvoices = statResponse.getData().getInvoices();
                nextInvoices.forEach(i -> purposes.put(i.getId(), i.getProduct()));
                continuationToken = Optional.ofNullable(statResponse.getContinuationToken());
            } while (continuationToken.isPresent());
            return purposes;
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
                    .orElseThrow(() -> new InvoiceNotFoundException(String.format("Invoice not found, invoiceId='%s'", invoiceId)));
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ShopAccountingModel getShopAccounting(String partyId, String shopId, String currencyCode, Optional<Instant> fromTime, Instant toTime) {
        try {
            ShopAccountingModel shopAccounting = merchantStatisticsClient.getStatistics(
                    DslUtil.createShopAccountingStatRequest(partyId, shopId, currencyCode, fromTime, toTime, objectMapper)
            ).getData()
                    .getRecords()
                    .stream()
                    .map(record -> objectMapper.convertValue(record, ShopAccountingModel.class))
                    .findFirst()
                    .orElse(new ShopAccountingModel(partyId, shopId, currencyCode));
            validate(shopAccounting);
            return shopAccounting;
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Iterator<StatPayment> getCapturedPaymentsIterator(String partyId, String shopId, Instant fromTime, Instant toTime) {
        return new Iterator<>() {
            private Optional<String> continuationToken = Optional.empty();
            private final int size = 1000;
            private Iterator<StatPayment> iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null || ((!iterator.hasNext()) && continuationToken.isPresent())) {
                    try {
                        StatResponse statResponse = merchantStatisticsClient.getPayments(DslUtil.createPaymentsRequest(partyId, shopId, fromTime, toTime, continuationToken, size, objectMapper));
                        iterator = statResponse.getData().getPayments().iterator();
                        continuationToken = Optional.ofNullable(statResponse.getContinuationToken());
                    } catch (TException e) {
                        throw new RuntimeException(e);
                    }
                }
                return iterator.hasNext();
            }

            @Override
            public StatPayment next() {
                return iterator.next();
            }
        };
    }

    @Override
    public StatPayment getCapturedPayment(String partyId, String shopId, String invoiceId, String paymentId) {
        try {
            return merchantStatisticsClient.getPayments(DslUtil.createPaymentRequest(partyId, shopId, invoiceId, paymentId, objectMapper))
                    .getData()
                    .getPayments()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new PaymentNotFoundException(String.format("Payment not found, invoiceId='%s', paymentId='%s'", invoiceId, paymentId)));
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Iterator<StatRefund> getRefundsIterator(String partyId, String shopId, Instant fromTime, Instant toTime) {
        return new Iterator<>() {
            private Optional<String> continuationToken = Optional.empty();
            private final int size = 1000;
            private Iterator<StatRefund> iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null || ((!iterator.hasNext()) && continuationToken.isPresent())) {
                    try {
                        StatResponse statResponse = merchantStatisticsClient.getPayments(DslUtil.createRefundsRequest(partyId, shopId, fromTime, toTime, continuationToken, size, objectMapper));
                        iterator = statResponse.getData().getRefunds().iterator();
                        continuationToken = Optional.ofNullable(statResponse.getContinuationToken());
                    } catch (TException e) {
                        throw new RuntimeException(e);
                    }
                }
                return iterator.hasNext();
            }

            @Override
            public StatRefund next() {
                return iterator.next();
            }
        };
    }

    private <T> void validate(T model) {
        Set<ConstraintViolation<T>> constraintViolations = factory.getValidator().validate(model);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }
}
