package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rbkmoney.damsel.merch_stat.InvoicePaymentRefundStatus;

import java.time.Instant;

public class RefundsQuery {

    @JsonProperty("merchant_id")
    String merchantId;

    @JsonProperty("contract_id")
    String contractId;

    @JsonProperty("from_time")
    Instant fromTime;

    @JsonProperty("to_time")
    Instant toTime;

    @JsonProperty("refund_status")
    String refundStatus;

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

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefundsQuery that = (RefundsQuery) o;

        if (merchantId != null ? !merchantId.equals(that.merchantId) : that.merchantId != null) return false;
        if (contractId != null ? !contractId.equals(that.contractId) : that.contractId != null) return false;
        if (fromTime != null ? !fromTime.equals(that.fromTime) : that.fromTime != null) return false;
        if (toTime != null ? !toTime.equals(that.toTime) : that.toTime != null) return false;
        return refundStatus != null ? refundStatus.equals(that.refundStatus) : that.refundStatus == null;
    }

    @Override
    public int hashCode() {
        int result = merchantId != null ? merchantId.hashCode() : 0;
        result = 31 * result + (contractId != null ? contractId.hashCode() : 0);
        result = 31 * result + (fromTime != null ? fromTime.hashCode() : 0);
        result = 31 * result + (toTime != null ? toTime.hashCode() : 0);
        result = 31 * result + (refundStatus != null ? refundStatus.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RefundsQuery{" +
                "merchantId='" + merchantId + '\'' +
                ", contractId='" + contractId + '\'' +
                ", fromTime=" + fromTime +
                ", toTime=" + toTime +
                ", refundStatus=" + refundStatus +
                '}';
    }
}
