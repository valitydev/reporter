package com.rbkmoney.reporter.model;

public class Refund {
    private String id;
    private String paymentId;
    private String paymentCapturedAt;
    private String succeededAt;
    private String paymentTool;
    private Long amount;
    private String payerEmail;
    private String shopUrl;
    private String paymentPurpose;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentCapturedAt() {
        return paymentCapturedAt;
    }

    public void setPaymentCapturedAt(String paymentCapturedAt) {
        this.paymentCapturedAt = paymentCapturedAt;
    }

    public String getSucceededAt() {
        return succeededAt;
    }

    public void setSucceededAt(String succeededAt) {
        this.succeededAt = succeededAt;
    }

    public String getPaymentTool() {
        return paymentTool;
    }

    public void setPaymentTool(String paymentTool) {
        this.paymentTool = paymentTool;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getShopUrl() {
        return shopUrl;
    }

    public void setShopUrl(String shopUrl) {
        this.shopUrl = shopUrl;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public void setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
    }
}
