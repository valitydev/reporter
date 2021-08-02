package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.config.PostgresqlSpringBootITest;
import com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import org.jooq.Cursor;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.testcontainers.annotations.util.RandomBeans.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class PaymentDaoTest {

    @Autowired
    private PaymentDao paymentDao;

    @Test
    public void saveAndGetPaymentTest() {
        String partyId = random(String.class);
        String shopId = random(String.class);

        int paymentsCount = 100;
        List<Payment> sourcePayments = new ArrayList<>();
        for (int i = 0; i < paymentsCount; i++) {
            Payment payment = random(Payment.class);
            payment.setShopId(shopId);
            payment.setPartyId(partyId);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setStatusCreatedAt(LocalDateTime.now());
            payment.setStatus(InvoicePaymentStatus.captured);
            sourcePayments.add(payment);
            paymentDao.savePayment(payment);
        }
        Payment firstPayment = sourcePayments.get(0);
        PaymentRecord payment =
                paymentDao.getPayment(partyId, shopId, firstPayment.getInvoiceId(), firstPayment.getPaymentId());
        assertEquals(firstPayment, payment.into(Payment.class));

        Cursor<PaymentRecord> paymentsCursor =
                paymentDao.getPaymentsCursor(partyId, shopId, Optional.empty(), LocalDateTime.now());
        List<Payment> resultPayments = new ArrayList<>();
        while (paymentsCursor.hasNext()) {
            Result<PaymentRecord> paymentRecords = paymentsCursor.fetchNext(10);
            resultPayments.addAll(paymentRecords.into(Payment.class));
        }
        assertEquals(paymentsCount, resultPayments.size());
    }
}
