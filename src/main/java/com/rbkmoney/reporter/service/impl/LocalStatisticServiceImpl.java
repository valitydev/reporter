package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.reporter.dao.*;
import com.rbkmoney.reporter.domain.tables.pojos.Invoice;
import com.rbkmoney.reporter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.reporter.domain.tables.records.InvoiceRecord;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.rbkmoney.reporter.domain.tables.records.RefundRecord;
import com.rbkmoney.reporter.exception.PaymentNotFoundException;
import com.rbkmoney.reporter.model.PaymentFundsAmount;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.LocalStatisticService;
import lombok.RequiredArgsConstructor;
import org.jooq.Cursor;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LocalStatisticServiceImpl implements LocalStatisticService {

    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;
    private final RefundDao refundDao;
    private final AdjustmentDao adjustmentDao;
    private final ChargebackDao chargebackDao;
    private final PayoutDao payoutDao;

    private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    @Override
    public ShopAccountingModel getShopAccounting(String partyId,
                                                 String shopId,
                                                 String currencyCode,
                                                 LocalDateTime toTime) {
        return getShopAccounting(partyId, shopId, currencyCode, Optional.empty(), toTime);
    }

    @Override
    public ShopAccountingModel getShopAccounting(String partyId,
                                                 String shopId,
                                                 String currencyCode,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime) {
        return getShopAccounting(partyId, shopId, currencyCode, Optional.of(fromTime), toTime);
    }

    private ShopAccountingModel getShopAccounting(String partyId,
                                                  String shopId,
                                                  String currencyCode,
                                                  Optional<LocalDateTime> fromTime,
                                                  LocalDateTime toTime) {
        ShopAccountingModel shopAccountingModel = new ShopAccountingModel(partyId, shopId, currencyCode);
        PaymentFundsAmount paymentFundsAmount =
                paymentDao.getPaymentFundsAmount(partyId, shopId, currencyCode, fromTime, toTime);
        shopAccountingModel.setFundsAcquired(paymentFundsAmount.getFundsAcquiredAmount());
        shopAccountingModel.setFeeCharged(paymentFundsAmount.getFeeChargedAmount());
        shopAccountingModel.setFundsRefunded(
                refundDao.getFundsRefundedAmount(partyId, shopId, currencyCode, fromTime, toTime));
        shopAccountingModel.setFundsPaidOut(
                payoutDao.getFundsPayOutAmount(partyId, shopId, currencyCode, fromTime, toTime));
        shopAccountingModel.setFundsAdjusted(
                adjustmentDao.getFundsAdjustedAmount(partyId, shopId, currencyCode, fromTime, toTime));
        shopAccountingModel.setFundsReturned(
                chargebackDao.getFundsReturnedAmount(partyId, shopId, currencyCode, fromTime, toTime));
        validate(shopAccountingModel);
        return shopAccountingModel;
    }

    @Override
    public Map<String, String> getPurposes(String partyId,
                                           String shopId,
                                           LocalDateTime fromTime,
                                           LocalDateTime toTime) {
        Map<String, String> purposes = new HashMap<>();
        List<Invoice> invoices = invoiceDao.getInvoices(
                partyId,
                shopId,
                Optional.ofNullable(fromTime),
                toTime
        );
        invoices.forEach(invoice -> purposes.put(invoice.getInvoiceId(), invoice.getProduct()));
        return purposes;
    }

    @Override
    public InvoiceRecord getInvoice(String invoiceId) {
        return invoiceDao.getInvoice(invoiceId);
    }

    @Override
    public Cursor<PaymentRecord> getPaymentsCursor(String partyId,
                                                   String shopId,
                                                   LocalDateTime fromTime,
                                                   LocalDateTime toTime) {
        return paymentDao.getPaymentsCursor(
                partyId,
                shopId,
                Optional.ofNullable(fromTime),
                toTime
        );
    }

    @Override
    public PaymentRecord getCapturedPayment(String partyId,
                                            String shopId,
                                            String invoiceId,
                                            String paymentId) {
        PaymentRecord payment = paymentDao.getPayment(partyId, shopId, invoiceId, paymentId);
        if (payment == null) {
            throw new PaymentNotFoundException(String.format("Payment not found, " +
                    "invoiceId='%s', paymentId='%s'", invoiceId, paymentId));
        }
        return payment;
    }

    @Override
    public Cursor<RefundRecord> getRefundsCursor(String partyId,
                                                 String shopId,
                                                 LocalDateTime fromTime,
                                                 LocalDateTime toTime) {
        return refundDao.getRefundsCursor(partyId, shopId, fromTime, toTime);
    }

    @Override
    public Cursor<AdjustmentRecord> getAdjustmentCursor(String partyId,
                                                        String shopId,
                                                        LocalDateTime fromTime,
                                                        LocalDateTime toTime) {
        return adjustmentDao.getAdjustmentCursor(partyId, shopId, fromTime, toTime);
    }

    private <T> void validate(T model) {
        Set<ConstraintViolation<T>> constraintViolations = factory.getValidator().validate(model);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

}
