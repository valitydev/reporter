package com.rbkmoney.reporter.model;

import com.rbkmoney.damsel.domain.CategoryType;

import java.util.Date;

/**
 * Created by tolkonepiu on 17/07/2017.
 */
public class PartyModel {

    private String merchantId;

    private String merchantName;

    private String merchantContractId;

    private Date merchantContractSignedAt;

    private String merchantRepresentativePosition;

    private String merchantRepresentativeFullName;

    private String shopId;

    private String shopName;

    private String shopDescription;

    private CategoryType shopCategoryType;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantContractId() {
        return merchantContractId;
    }

    public void setMerchantContractId(String merchantContractId) {
        this.merchantContractId = merchantContractId;
    }

    public Date getMerchantContractSignedAt() {
        return merchantContractSignedAt;
    }

    public void setMerchantContractSignedAt(Date merchantContractSignedAt) {
        this.merchantContractSignedAt = merchantContractSignedAt;
    }

    public String getMerchantRepresentativePosition() {
        return merchantRepresentativePosition;
    }

    public void setMerchantRepresentativePosition(String merchantRepresentativePosition) {
        this.merchantRepresentativePosition = merchantRepresentativePosition;
    }

    public String getMerchantRepresentativeFullName() {
        return merchantRepresentativeFullName;
    }

    public void setMerchantRepresentativeFullName(String merchantRepresentativeFullName) {
        this.merchantRepresentativeFullName = merchantRepresentativeFullName;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public void setShopDescription(String shopDescription) {
        this.shopDescription = shopDescription;
    }

    public CategoryType getShopCategoryType() {
        return shopCategoryType;
    }

    public void setShopCategoryType(CategoryType shopCategoryType) {
        this.shopCategoryType = shopCategoryType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PartyModel that = (PartyModel) o;

        if (merchantId != null ? !merchantId.equals(that.merchantId) : that.merchantId != null) return false;
        if (merchantName != null ? !merchantName.equals(that.merchantName) : that.merchantName != null) return false;
        if (merchantContractId != null ? !merchantContractId.equals(that.merchantContractId) : that.merchantContractId != null)
            return false;
        if (merchantContractSignedAt != null ? !merchantContractSignedAt.equals(that.merchantContractSignedAt) : that.merchantContractSignedAt != null)
            return false;
        if (merchantRepresentativePosition != null ? !merchantRepresentativePosition.equals(that.merchantRepresentativePosition) : that.merchantRepresentativePosition != null)
            return false;
        if (merchantRepresentativeFullName != null ? !merchantRepresentativeFullName.equals(that.merchantRepresentativeFullName) : that.merchantRepresentativeFullName != null)
            return false;
        if (shopId != null ? !shopId.equals(that.shopId) : that.shopId != null) return false;
        if (shopName != null ? !shopName.equals(that.shopName) : that.shopName != null) return false;
        if (shopDescription != null ? !shopDescription.equals(that.shopDescription) : that.shopDescription != null)
            return false;
        return shopCategoryType == that.shopCategoryType;
    }

    @Override
    public int hashCode() {
        int result = merchantId != null ? merchantId.hashCode() : 0;
        result = 31 * result + (merchantName != null ? merchantName.hashCode() : 0);
        result = 31 * result + (merchantContractId != null ? merchantContractId.hashCode() : 0);
        result = 31 * result + (merchantContractSignedAt != null ? merchantContractSignedAt.hashCode() : 0);
        result = 31 * result + (merchantRepresentativePosition != null ? merchantRepresentativePosition.hashCode() : 0);
        result = 31 * result + (merchantRepresentativeFullName != null ? merchantRepresentativeFullName.hashCode() : 0);
        result = 31 * result + (shopId != null ? shopId.hashCode() : 0);
        result = 31 * result + (shopName != null ? shopName.hashCode() : 0);
        result = 31 * result + (shopDescription != null ? shopDescription.hashCode() : 0);
        result = 31 * result + (shopCategoryType != null ? shopCategoryType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PartyModel{" +
                "merchantId='" + merchantId + '\'' +
                ", merchantName='" + merchantName + '\'' +
                ", merchantContractId='" + merchantContractId + '\'' +
                ", merchantContractSignedAt=" + merchantContractSignedAt +
                ", merchantRepresentativePosition='" + merchantRepresentativePosition + '\'' +
                ", merchantRepresentativeFullName='" + merchantRepresentativeFullName + '\'' +
                ", shopId='" + shopId + '\'' +
                ", shopName='" + shopName + '\'' +
                ", shopDescription='" + shopDescription + '\'' +
                ", shopCategoryType=" + shopCategoryType +
                '}';
    }
}
