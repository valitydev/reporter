package com.rbkmoney.reporter.model;

public class Payment {
    private String id;
    private String capturedAt;
    private String paymentTool;
    private Double amount;
    private Double payoutAmount;
    private String payerEmail;
    private String shopUrl;
    private String purpose;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(String capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getPaymentTool() {
        return paymentTool;
    }

    public void setPaymentTool(String paymentTool) {
        this.paymentTool = paymentTool;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(Double payoutAmount) {
        this.payoutAmount = payoutAmount;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
