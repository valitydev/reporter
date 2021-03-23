package com.rbkmoney.reporter.dao;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AggregatesDao {

    Optional<LocalDateTime> getLastAggregationDate();

    void aggregateForDate(LocalDateTime dateFrom, LocalDateTime dateTo);

}
