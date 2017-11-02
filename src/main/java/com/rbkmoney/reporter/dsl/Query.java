package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by tolkonepiu on 10/07/2017.
 */
public class Query {

    @JsonProperty("shop_accounting_report")
    private ShopAccountingQuery shopAccountingQuery;

    @JsonProperty("payments")
    private PaymentsQuery paymentsQuery;

    public ShopAccountingQuery getShopAccountingQuery() {
        return shopAccountingQuery;
    }

    public void setShopAccountingQuery(ShopAccountingQuery shopAccountingQuery) {
        this.shopAccountingQuery = shopAccountingQuery;
    }

    public PaymentsQuery getPaymentsQuery() {
        return paymentsQuery;
    }

    public void setPaymentsQuery(PaymentsQuery paymentsQuery) {
        this.paymentsQuery = paymentsQuery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Query query = (Query) o;

        if (shopAccountingQuery != null ? !shopAccountingQuery.equals(query.shopAccountingQuery) : query.shopAccountingQuery != null)
            return false;
        return paymentsQuery != null ? paymentsQuery.equals(query.paymentsQuery) : query.paymentsQuery == null;
    }

    @Override
    public int hashCode() {
        int result = shopAccountingQuery != null ? shopAccountingQuery.hashCode() : 0;
        result = 31 * result + (paymentsQuery != null ? paymentsQuery.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Query{" +
                "shopAccountingQuery=" + shopAccountingQuery +
                ", paymentsQuery=" + paymentsQuery +
                '}';
    }
}
