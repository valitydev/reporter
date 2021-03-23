package com.rbkmoney.reporter.dao;

import java.time.LocalDateTime;

public interface AggregatesDao {

    LocalDateTime getLastAggregationDate();

    void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo);

}
