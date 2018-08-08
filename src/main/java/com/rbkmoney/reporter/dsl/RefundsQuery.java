package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rbkmoney.damsel.merch_stat.InvoicePaymentRefundStatus;

import java.time.Instant;
import java.util.Objects;

public class RefundsQuery {

    @JsonProperty("merchant_id")
    String merchantId;

    @JsonProperty("shop_id")
    String shopId;

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

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
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
        return Objects.equals(merchantId, that.merchantId) &&
                Objects.equals(shopId, that.shopId) &&
                Objects.equals(fromTime, that.fromTime) &&
                Objects.equals(toTime, that.toTime) &&
                Objects.equals(refundStatus, that.refundStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchantId, shopId, fromTime, toTime, refundStatus);
    }

    @Override
    public String toString() {
        return "RefundsQuery{" +
                "merchantId='" + merchantId + '\'' +
                ", shopId='" + shopId + '\'' +
                ", fromTime=" + fromTime +
                ", toTime=" + toTime +
                ", refundStatus='" + refundStatus + '\'' +
                '}';
    }
}
