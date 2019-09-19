package com.rbkmoney.reporter.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Created by tolkonepiu on 17/07/2017.
 */
public class ShopAccountingModel {

    @NotNull
    private String merchantId;

    @NotNull
    private String shopId;

    @NotNull
    private String currencyCode;

    @Min(0)
    private long fundsAcquired;

    @Min(0)
    private long feeCharged;

    private long fundsAdjusted;

    @Min(0)
    private long fundsPaidOut;

    @Min(0)
    private long fundsRefunded;

    public ShopAccountingModel(String merchantId, String shopId, String currencyCode) {
        this.merchantId = merchantId;
        this.shopId = shopId;
        this.currencyCode = currencyCode;
    }

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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public long getFundsAcquired() {
        return fundsAcquired;
    }

    public void setFundsAcquired(long fundsAcquired) {
        this.fundsAcquired = fundsAcquired;
    }

    public long getFeeCharged() {
        return feeCharged;
    }

    public void setFeeCharged(long feeCharged) {
        this.feeCharged = feeCharged;
    }

    public long getFundsPaidOut() {
        return fundsPaidOut;
    }

    public void setFundsPaidOut(long fundsPaidOut) {
        this.fundsPaidOut = fundsPaidOut;
    }

    public long getFundsAdjusted() {
        return fundsAdjusted;
    }

    public void setFundsAdjusted(long fundsAdjusted) {
        this.fundsAdjusted = fundsAdjusted;
    }

    public long getFundsRefunded() {
        return fundsRefunded;
    }

    public void setFundsRefunded(long fundsRefunded) {
        this.fundsRefunded = fundsRefunded;
    }

    public long getAvailableFunds() {
        return fundsAcquired + fundsAdjusted - feeCharged - fundsPaidOut - fundsRefunded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopAccountingModel that = (ShopAccountingModel) o;
        return fundsAcquired == that.fundsAcquired &&
                feeCharged == that.feeCharged &&
                fundsAdjusted == that.fundsAdjusted &&
                fundsPaidOut == that.fundsPaidOut &&
                fundsRefunded == that.fundsRefunded &&
                Objects.equals(merchantId, that.merchantId) &&
                Objects.equals(shopId, that.shopId) &&
                Objects.equals(currencyCode, that.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchantId, shopId, currencyCode, fundsAcquired, feeCharged, fundsAdjusted, fundsPaidOut, fundsRefunded);
    }

    @Override
    public String toString() {
        return "ShopAccountingModel{" +
                "merchantId='" + merchantId + '\'' +
                ", shopId='" + shopId + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", fundsAcquired=" + fundsAcquired +
                ", feeCharged=" + feeCharged +
                ", fundsAdjusted=" + fundsAdjusted +
                ", fundsPaidOut=" + fundsPaidOut +
                ", fundsRefunded=" + fundsRefunded +
                '}';
    }
}
