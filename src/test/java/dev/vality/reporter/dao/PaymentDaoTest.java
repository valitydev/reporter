package dev.vality.reporter.dao;

import dev.vality.reporter.config.PostgresqlSpringBootITest;
import dev.vality.reporter.domain.enums.InvoicePaymentStatus;
import dev.vality.reporter.domain.tables.pojos.Payment;
import dev.vality.reporter.domain.tables.records.PaymentRecord;
import org.jooq.Cursor;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.vality.testcontainers.annotations.util.RandomBeans.random;
import static dev.vality.testcontainers.annotations.util.RandomBeans.randomListOf;
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
        List<Payment> sourcePayments = randomListOf(paymentsCount, Payment.class);
        sourcePayments.forEach(payment -> {
            payment.setShopId(shopId);
            payment.setPartyId(partyId);
            payment.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
            payment.setStatusCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
            payment.setStatus(InvoicePaymentStatus.captured);
            paymentDao.savePayment(payment);
        });
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
