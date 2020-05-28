package com.rbkmoney.reporter.dao;

import com.rbkmoney.reporter.domain.enums.InvoicePaymentStatus;
import com.rbkmoney.reporter.domain.tables.pojos.Payment;
import com.rbkmoney.reporter.domain.tables.pojos.PaymentAdditionalInfo;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentDao {

    Long savePayment(Payment payment);

    List<Payment> getPaymentsByState(LocalDateTime dateFrom,
                                     LocalDateTime dateTo,
                                     List<InvoicePaymentStatus> statuses);

    void saveAdditionalPaymentInfo(PaymentAdditionalInfo paymentAdditionalInfo);

}
