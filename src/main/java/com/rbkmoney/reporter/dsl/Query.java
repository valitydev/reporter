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

    @JsonProperty("payments_for_report")
    private PaymentsForReportQuery paymentsForReportQuery;

    @JsonProperty("refunds_for_report")
    private RefundsForReportQuery refundsForReportQuery;

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

    public PaymentsForReportQuery getPaymentsForReportQuery() {
        return paymentsForReportQuery;
    }

    public void setPaymentsForReportQuery(PaymentsForReportQuery paymentsForReportQuery) {
        this.paymentsForReportQuery = paymentsForReportQuery;
    }

    public RefundsForReportQuery getRefundsForReportQuery() {
        return refundsForReportQuery;
    }

    public void setRefundsForReportQuery(RefundsForReportQuery refundsForReportQuery) {
        this.refundsForReportQuery = refundsForReportQuery;
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
        if (o == null || getClass() != o.getClass()) return false;
        Query query = (Query) o;
        return Objects.equals(shopAccountingQuery, query.shopAccountingQuery) &&
                Objects.equals(invoicesQuery, query.invoicesQuery) &&
                Objects.equals(paymentsForReportQuery, query.paymentsForReportQuery) &&
                Objects.equals(refundsForReportQuery, query.refundsForReportQuery) &&
                Objects.equals(refundsQuery, query.refundsQuery) &&
                Objects.equals(from, query.from) &&
                Objects.equals(size, query.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shopAccountingQuery, invoicesQuery, paymentsForReportQuery, refundsForReportQuery, refundsQuery, from, size);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Query{");
        sb.append("shopAccountingQuery=").append(shopAccountingQuery);
        sb.append(", invoicesQuery=").append(invoicesQuery);
        sb.append(", paymentsForReportQuery=").append(paymentsForReportQuery);
        sb.append(", refundsForReportQuery=").append(refundsForReportQuery);
        sb.append(", refundsQuery=").append(refundsQuery);
        sb.append(", from=").append(from);
        sb.append(", size=").append(size);
        sb.append('}');
        return sb.toString();
    }
}
