package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class InvoicesQuery {

    @JsonProperty("merchant_id")
    String merchantId;

    @JsonProperty("contract_id")
    String contractId;

    @JsonProperty("invoice_id")
    String invoiceId;

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
        if (!(o instanceof InvoicesQuery)) return false;
        InvoicesQuery that = (InvoicesQuery) o;
        return Objects.equals(getMerchantId(), that.getMerchantId()) &&
                Objects.equals(getContractId(), that.getContractId()) &&
                Objects.equals(getInvoiceId(), that.getInvoiceId()) &&
                Objects.equals(getFromTime(), that.getFromTime()) &&
                Objects.equals(getToTime(), that.getToTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMerchantId(), getContractId(), getInvoiceId(), getFromTime(), getToTime());
    }

    @Override
    public String toString() {
        return "PaymentsQuery{" +
                "merchantId='" + merchantId + '\'' +
                ", contractId='" + contractId + '\'' +
                ", invoiceId='" + invoiceId + '\'' +
                ", fromTime=" + fromTime +
                ", toTime=" + toTime +
                '}';
    }
}
