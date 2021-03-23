package com.rbkmoney.reporter.service;

public interface AggregationService {

    void aggregatePayments();

    void aggregateRefunds();

    void aggregateAdjustments();

    void aggregatePayouts();

}
