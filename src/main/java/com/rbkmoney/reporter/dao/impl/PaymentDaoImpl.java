package com.rbkmoney.reporter.dao.impl;

import com.rbkmoney.reporter.dao.AbstractDao;
import com.rbkmoney.reporter.dao.PaymentDao;
import com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.PaymentAdditionalInfo;
import com.rbkmoney.reporter.domain.tables.records.PaymentRecord;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Cursor;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.rbkmoney.reporter.domain.tables.Payment.PAYMENT;
import static com.rbkmoney.reporter.domain.tables.PaymentAdditionalInfo.PAYMENT_ADDITIONAL_INFO;

@Component
public class PaymentDaoImpl extends AbstractDao implements PaymentDao {

    @Autowired
    public PaymentDaoImpl(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Long savePayment(Payment payment) {
        return getDslContext()
                .insertInto(PAYMENT)
                .set(getDslContext().newRecord(PAYMENT, payment))
                .onConflict(PAYMENT.INVOICE_ID, PAYMENT.PAYMENT_ID)
                .doUpdate()
                .set(getDslContext().newRecord(PAYMENT, payment))
                .returning(PAYMENT.ID)
                .fetchOne()
                .getId();
    }

    @Override
    public List<Payment> getPaymentsByState(LocalDateTime dateFrom,
                                            LocalDateTime dateTo,
                                            List<InvoicePaymentStatus> statuses) {
        Result<PaymentRecord> records = getDslContext()
                .selectFrom(PAYMENT)
                .where(PAYMENT.STATUS_CREATED_AT.greaterThan(dateFrom)
                        .and(PAYMENT.STATUS_CREATED_AT.lessThan(dateTo))
                        .and(PAYMENT.STATUS.in(statuses)))
                .fetch();
        return records == null || records.isEmpty() ? new ArrayList<>() : records.into(Payment.class);
    }

    @Override
    public void saveAdditionalPaymentInfo(PaymentAdditionalInfo paymentAdditionalInfo) {
        getDslContext()
                .insertInto(PAYMENT_ADDITIONAL_INFO)
                .set(getDslContext().newRecord(PAYMENT_ADDITIONAL_INFO, paymentAdditionalInfo))
                .onDuplicateKeyUpdate()
                .set(getDslContext().newRecord(PAYMENT_ADDITIONAL_INFO, paymentAdditionalInfo))
                .execute();
    }

    @Override
    public Map<String, Long> getShopAccountingReportData(String partyId,
                                                         String partyShopId,
                                                         String currencyCode,
                                                         Optional<LocalDateTime> fromTime,
                                                         LocalDateTime toTime) {
        String amountKey = "funds_acquired";
        String feeKey = "fee_charged";

        var conditionStep = getDslContext().select(
                DSL.sum(PAYMENT.AMOUNT).as(amountKey),
                DSL.sum(DSL.coalesce(PAYMENT.FEE, 0L)).as(feeKey)
        )
                .from(PAYMENT)
                .where(fromTime.map(PAYMENT.CREATED_AT::ge).orElse(DSL.trueCondition()))
                .and(PAYMENT.CREATED_AT.lt(toTime))
                .and(PAYMENT.STATUS.eq(InvoicePaymentStatus.captured))
                .and(PAYMENT.CURRENCY_CODE.eq(currencyCode))
                .and(PAYMENT.PARTY_ID.eq(partyId))
                .and(PAYMENT.SHOP_ID.eq(partyShopId));

        Record2<BigDecimal, BigDecimal> result = conditionStep.fetchOne();
        Map<String, Long> accountingData = new HashMap<>();
        if (result == null) {
            accountingData.put(amountKey, 0L);
            accountingData.put(feeKey, 0L);
        } else {
            accountingData.put(amountKey, result.value1().longValue());
            accountingData.put(feeKey, result.value2().longValue());
        }
        return accountingData;
    }

    @Override
    public PaymentRecord getPayment(String partyId,
                                    String shopId,
                                    String invoiceId,
                                    String paymentId) {
        return getDslContext()
                .selectFrom(PAYMENT)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId))
                .and(PAYMENT.PAYMENT_ID.eq(paymentId))
                .and(PAYMENT.PARTY_ID.eq(partyId))
                .and(PAYMENT.SHOP_ID.eq(shopId))
                .fetchOne();
    }

    @Override
    public Cursor<PaymentRecord> getPaymentsCursor(String partyId,
                                                   String shopId,
                                                   Optional<LocalDateTime> fromTime,
                                                   LocalDateTime toTime) {
        return getDslContext()
                .selectFrom(PAYMENT)
                .where(fromTime.map(PAYMENT.STATUS_CREATED_AT::ge).orElse(DSL.trueCondition()))
                .and(PAYMENT.STATUS_CREATED_AT.lt(toTime))
                .and(PAYMENT.PARTY_ID.eq(partyId))
                .and(PAYMENT.SHOP_ID.eq(shopId))
                .and(PAYMENT.STATUS.eq(InvoicePaymentStatus.captured))
                .fetchLazy();
    }

}
