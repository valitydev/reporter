package com.rbkmoney.reporter.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Objects;

@Data
public class Query {

    @JsonProperty("shop_accounting_report")
    private ShopAccountingQuery shopAccountingQuery;

    @JsonProperty("invoices")
    private InvoicesQuery invoicesQuery;

    @JsonProperty("payments_for_report")
    private PaymentsForReportQuery paymentsForReportQuery;

    @JsonProperty("refunds_for_report")
    private RefundsForReportQuery refundsForReportQuery;

    @JsonProperty("adjustments_for_report")
    private AdjustmentsForReportQuery adjustmentsForReportQuery;

    @JsonProperty("refunds")
    private RefundsQuery refundsQuery;

    private Long from;

    private Integer size;

}
