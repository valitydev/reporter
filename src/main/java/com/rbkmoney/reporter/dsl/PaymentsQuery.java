package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rbkmoney.damsel.merch_stat.InvoicePaymentStatus;

import java.time.Instant;
import java.util.Objects;

public class PaymentsQuery {

    @JsonProperty("merchant_id")
    String merchantId;

    @JsonProperty("contract_id")
    String contractId;

    @JsonProperty("invoice_id")
    String invoiceId;

    @JsonProperty("payment_id")
    String paymentId;

    @JsonProperty("from_time")
    Instant fromTime;

    @JsonProperty("to_time")
    Instant toTime;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Instant getFromTime() {
        return fromTime;
    }

    public void setFromTime(Instant fromTime) {
        this.fromTime = fromTime;
    }

    public Instant getToTime() {
        return toTime;
    }

    public void setToTime(Instant toTime) {
        this.toTime = toTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentsQuery that = (PaymentsQuery) o;
        return Objects.equals(merchantId, that.merchantId) &&
                Objects.equals(contractId, that.contractId) &&
                Objects.equals(invoiceId, that.invoiceId) &&
                Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(fromTime, that.fromTime) &&
                Objects.equals(toTime, that.toTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchantId, contractId, invoiceId, paymentId, fromTime, toTime);
    }

    @Override
    public String toString() {
        return "PaymentsQuery{" +
                "merchantId='" + merchantId + '\'' +
                ", contractId='" + contractId + '\'' +
                ", invoiceId='" + invoiceId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", fromTime=" + fromTime +
                ", toTime=" + toTime +
                '}';
    }
}
