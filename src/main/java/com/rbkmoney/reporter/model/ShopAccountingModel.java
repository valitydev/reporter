package com.rbkmoney.reporter.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rbkmoney.reporter.serializer.CurrencyDeserializer;

/**
 * Created by tolkonepiu on 17/07/2017.
 */
public class ShopAccountingModel {

    private String merchantId;

    private String shopId;

    private String currencyCode;

    @JsonDeserialize(using = CurrencyDeserializer.class)
    private double fundsAcquired;

    @JsonDeserialize(using = CurrencyDeserializer.class)
    private double feeCharged;

    @JsonDeserialize(using = CurrencyDeserializer.class)
    private double openingBalance;

    @JsonDeserialize(using = CurrencyDeserializer.class)
    private double fundsPaidOut;

    @JsonDeserialize(using = CurrencyDeserializer.class)
    private double fundsRefunded;

    @JsonDeserialize(using = CurrencyDeserializer.class)
    private double closingBalance;

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

    public double getFundsAcquired() {
        return fundsAcquired;
    }

    public void setFundsAcquired(double fundsAcquired) {
        this.fundsAcquired = fundsAcquired;
    }

    public double getFeeCharged() {
        return feeCharged;
    }

    public void setFeeCharged(double feeCharged) {
        this.feeCharged = feeCharged;
    }

    public double getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(double openingBalance) {
        this.openingBalance = openingBalance;
    }

    public double getFundsPaidOut() {
        return fundsPaidOut;
    }

    public void setFundsPaidOut(double fundsPaidOut) {
        this.fundsPaidOut = fundsPaidOut;
    }

    public double getFundsRefunded() {
        return fundsRefunded;
    }

    public void setFundsRefunded(double fundsRefunded) {
        this.fundsRefunded = fundsRefunded;
    }

    public double getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(double closingBalance) {
        this.closingBalance = closingBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShopAccountingModel that = (ShopAccountingModel) o;

        if (Double.compare(that.fundsAcquired, fundsAcquired) != 0) return false;
        if (Double.compare(that.feeCharged, feeCharged) != 0) return false;
        if (Double.compare(that.openingBalance, openingBalance) != 0) return false;
        if (Double.compare(that.fundsPaidOut, fundsPaidOut) != 0) return false;
        if (Double.compare(that.fundsRefunded, fundsRefunded) != 0) return false;
        if (Double.compare(that.closingBalance, closingBalance) != 0) return false;
        if (merchantId != null ? !merchantId.equals(that.merchantId) : that.merchantId != null) return false;
        if (shopId != null ? !shopId.equals(that.shopId) : that.shopId != null) return false;
        return currencyCode != null ? currencyCode.equals(that.currencyCode) : that.currencyCode == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = merchantId != null ? merchantId.hashCode() : 0;
        result = 31 * result + (shopId != null ? shopId.hashCode() : 0);
        result = 31 * result + (currencyCode != null ? currencyCode.hashCode() : 0);
        temp = Double.doubleToLongBits(fundsAcquired);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(feeCharged);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(openingBalance);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(fundsPaidOut);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(fundsRefunded);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(closingBalance);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ShopAccountingModel{" +
                "merchantId='" + merchantId + '\'' +
                ", shopId='" + shopId + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", fundsAcquired=" + fundsAcquired +
                ", feeCharged=" + feeCharged +
                ", openingBalance=" + openingBalance +
                ", fundsPaidOut=" + fundsPaidOut +
                ", fundsRefunded=" + fundsRefunded +
                ", closingBalance=" + closingBalance +
                '}';
    }
}
