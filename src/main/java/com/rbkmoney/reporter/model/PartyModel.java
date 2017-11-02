package com.rbkmoney.reporter.model;

import java.util.Date;

/**
 * Created by tolkonepiu on 17/07/2017.
 */
public class PartyModel {

    private String merchantId;

    private String merchantName;

    private String merchantContractId;

    private Date merchantContractCreatedAt;

    private String merchantRepresentativePosition;

    private String merchantRepresentativeFullName;

    private String merchantRepresentativeDocument;

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

    public Date getMerchantContractCreatedAt() {
        return merchantContractCreatedAt;
    }

    public void setMerchantContractCreatedAt(Date merchantContractCreatedAt) {
        this.merchantContractCreatedAt = merchantContractCreatedAt;
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

    public String getMerchantRepresentativeDocument() {
        return merchantRepresentativeDocument;
    }

    public void setMerchantRepresentativeDocument(String merchantRepresentativeDocument) {
        this.merchantRepresentativeDocument = merchantRepresentativeDocument;
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
        if (merchantContractCreatedAt != null ? !merchantContractCreatedAt.equals(that.merchantContractCreatedAt) : that.merchantContractCreatedAt != null)
            return false;
        if (merchantRepresentativePosition != null ? !merchantRepresentativePosition.equals(that.merchantRepresentativePosition) : that.merchantRepresentativePosition != null)
            return false;
        if (merchantRepresentativeFullName != null ? !merchantRepresentativeFullName.equals(that.merchantRepresentativeFullName) : that.merchantRepresentativeFullName != null)
            return false;
        return merchantRepresentativeDocument != null ? merchantRepresentativeDocument.equals(that.merchantRepresentativeDocument) : that.merchantRepresentativeDocument == null;
    }

    @Override
    public int hashCode() {
        int result = merchantId != null ? merchantId.hashCode() : 0;
        result = 31 * result + (merchantName != null ? merchantName.hashCode() : 0);
        result = 31 * result + (merchantContractId != null ? merchantContractId.hashCode() : 0);
        result = 31 * result + (merchantContractCreatedAt != null ? merchantContractCreatedAt.hashCode() : 0);
        result = 31 * result + (merchantRepresentativePosition != null ? merchantRepresentativePosition.hashCode() : 0);
        result = 31 * result + (merchantRepresentativeFullName != null ? merchantRepresentativeFullName.hashCode() : 0);
        result = 31 * result + (merchantRepresentativeDocument != null ? merchantRepresentativeDocument.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PartyModel{" +
                "merchantId='" + merchantId + '\'' +
                ", merchantName='" + merchantName + '\'' +
                ", merchantContractId='" + merchantContractId + '\'' +
                ", merchantContractCreatedAt=" + merchantContractCreatedAt +
                ", merchantRepresentativePosition='" + merchantRepresentativePosition + '\'' +
                ", merchantRepresentativeFullName='" + merchantRepresentativeFullName + '\'' +
                ", merchantRepresentativeDocument='" + merchantRepresentativeDocument + '\'' +
                '}';
    }
}
