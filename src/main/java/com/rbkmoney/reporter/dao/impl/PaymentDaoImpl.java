package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.dao.impl.AbstractGenericDao;
import com.rbkmoney.mapper.RecordRowMapper;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.PaymentAdditionalInfo;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.reporter.domain.tables.Payment.PAYMENT;
import static com.rbkmoney.reporter.domain.tables.PaymentAdditionalInfo.PAYMENT_ADDITIONAL_INFO;

@Component
public class PaymentDaoImpl extends AbstractGenericDao implements PaymentDao {

    private final RowMapper<Payment> paymentRowMapper;

    @Autowired
    public PaymentDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
        paymentRowMapper = new RecordRowMapper<>(PAYMENT, Payment.class);
    }

    @Override
    public Long savePayment(Payment payment) {
        return fetchOne(getSavePaymentQuery(payment), Long.class);
    }

    @Override
    public Query getSavePaymentQuery(Payment payment) {
        return  getDslContext()
                .insertInto(PAYMENT)
                .set(getDslContext().newRecord(PAYMENT, payment))
                .onConflict(PAYMENT.INVOICE_ID, PAYMENT.PAYMENT_ID)
                .doNothing();
    }

    @Override
    public List<Payment> getPaymentsByState(LocalDateTime dateFrom,
                                            LocalDateTime dateTo,
                                            List<InvoicePaymentStatus> statuses) {
        Query query = getDslContext()
                .selectFrom(PAYMENT)
                .where(PAYMENT.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(PAYMENT.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(PAYMENT.STATUS.in(statuses)));
        return fetch(query, paymentRowMapper);
    }

    @Override
    public void saveAdditionalPaymentInfo(PaymentAdditionalInfo paymentAdditionalInfo) {
        executeOne(
                getSaveAdditionalPaymentInfoQuery(paymentAdditionalInfo)
        );
    }

    @Override
    public Query getSaveAdditionalPaymentInfoQuery(PaymentAdditionalInfo paymentAdditionalInfo) {
        return getDslContext()
                .insertInto(PAYMENT_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(PAYMENT_ADDITIONAL_INFO, paymentAdditionalInfo))
                .onConflict(PAYMENT_ADDITIONAL_INFO.INVOICE_ID, PAYMENT_ADDITIONAL_INFO.PAYMENT_ID)
                .doNothing();
    }
}
