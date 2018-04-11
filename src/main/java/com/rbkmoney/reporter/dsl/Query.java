package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Created by tolkonepiu on 10/07/2017.
 */
public class Query {

    @JsonProperty("shop_accounting_report")
    private ShopAccountingQuery shopAccountingQuery;

    @JsonProperty("invoices")
    private InvoicesQuery invoicesQuery;

    @JsonProperty("payments")
    private PaymentsQuery paymentsQuery;

    @JsonProperty("refunds")
    private RefundsQuery refundsQuery;

    private Long from;

    private Integer size;

    public ShopAccountingQuery getShopAccountingQuery() {
        return shopAccountingQuery;
    }

    public void setShopAccountingQuery(ShopAccountingQuery shopAccountingQuery) {
        this.shopAccountingQuery = shopAccountingQuery;
    }

    public InvoicesQuery getInvoicesQuery() {
        return invoicesQuery;
    }

    public void setInvoicesQuery(InvoicesQuery invoicesQuery) {
        this.invoicesQuery = invoicesQuery;
    }

    public PaymentsQuery getPaymentsQuery() {
        return paymentsQuery;
    }

    public void setPaymentsQuery(PaymentsQuery paymentsQuery) {
        this.paymentsQuery = paymentsQuery;
    }

    public RefundsQuery getRefundsQuery() {
        return refundsQuery;
    }

    public void setRefundsQuery(RefundsQuery refundsQuery) {
        this.refundsQuery = refundsQuery;
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;
        Query query = (Query) o;
        return getFrom() == query.getFrom() &&
                getSize() == query.getSize() &&
                Objects.equals(getShopAccountingQuery(), query.getShopAccountingQuery()) &&
                Objects.equals(getInvoicesQuery(), query.getInvoicesQuery()) &&
                Objects.equals(getPaymentsQuery(), query.getPaymentsQuery()) &&
                Objects.equals(getRefundsQuery(), query.getRefundsQuery());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getShopAccountingQuery(), getInvoicesQuery(), getPaymentsQuery(), getRefundsQuery(), getFrom(), getSize());
    }

    @Override
    public String toString() {
        return "Query{" +
                "shopAccountingQuery=" + shopAccountingQuery +
                ", invoicesQuery=" + invoicesQuery +
                ", paymentsQuery=" + paymentsQuery +
                ", refundsQuery=" + refundsQuery +
                ", from=" + from +
                ", size=" + size +
                '}';
    }
}
